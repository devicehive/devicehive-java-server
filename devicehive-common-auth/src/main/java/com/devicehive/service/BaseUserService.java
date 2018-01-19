package com.devicehive.service;

/*
 * #%L
 * DeviceHive Java Server Common business logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.NetworkDao;
import com.devicehive.dao.UserDao;
import com.devicehive.exceptions.HiveException;
import com.devicehive.exceptions.InvalidPrincipalException;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.service.configuration.ConfigurationService;
import com.devicehive.service.helpers.PasswordProcessor;
import com.devicehive.service.time.TimestampService;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.NetworkWithUsersAndDevicesVO;
import com.devicehive.vo.UserVO;
import com.devicehive.vo.UserWithDeviceTypeVO;
import com.devicehive.vo.UserWithNetworkVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.Objects;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

/**
 * This class serves all requests to database from controller.
 */
@Component
public class BaseUserService {

    private static final Logger logger = LoggerFactory.getLogger(BaseUserService.class);

    protected final PasswordProcessor passwordService;
    protected final UserDao userDao;
    protected final NetworkDao networkDao;
    protected final TimestampService timestampService;
    protected final ConfigurationService configurationService;
    protected final HiveValidator hiveValidator;
    
    @Autowired
    public BaseUserService(PasswordProcessor passwordService,
                       UserDao userDao,
                       NetworkDao networkDao,
                       TimestampService timestampService,
                       ConfigurationService configurationService,
                       HiveValidator hiveValidator) {
        this.passwordService = passwordService;
        this.userDao = userDao;
        this.networkDao = networkDao;
        this.timestampService = timestampService;
        this.configurationService = configurationService;
        this.hiveValidator = hiveValidator;
    }

    @Transactional(noRollbackFor = InvalidPrincipalException.class)
    public UserVO getActiveUser(String login, String password) {
        Optional<UserVO> userOpt = userDao.findByName(login);
        if (!userOpt.isPresent()) {
            logger.error("Can't find user with login {} and password {}", login, password);
            throw new InvalidPrincipalException(String.format(Messages.USER_LOGIN_NOT_FOUND, login));
        } else if (userOpt.get().getStatus() != UserStatus.ACTIVE) {
            logger.error("User with login {} is not active", login);
            throw new InvalidPrincipalException(Messages.USER_NOT_ACTIVE);
        }
        return checkPassword(userOpt.get(), password)
                .orElseThrow(() -> new InvalidPrincipalException(String.format(Messages.INCORRECT_CREDENTIALS, login)));
    }

    /**
     * Retrieves user by id (no networks fetched in this case)
     *
     * @param id user id
     * @return User model without networks, or null if there is no such user
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UserVO findById(@NotNull long id) {
        return userDao.find(id);
    }

    /**
     * Retrieves user with networks by id, if there is no networks user has
     * access to networks will be represented by empty set
     *
     * @param id user id
     * @return User model with networks, or null, if there is no such user
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UserWithNetworkVO findUserWithNetworks(@NotNull long id) {
        return userDao.getWithNetworksById(id);
    }

    /**
     * Retrieves user with device types by id, if there is no device types user has
     * access to device types will be represented by empty set
     *
     * @param id user id
     * @return User model with device types, or null, if there is no such user
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UserWithDeviceTypeVO findUserWithDeviceType(@NotNull long id) {
        return userDao.getWithDeviceTypeById(id);
    }

    @Transactional
    public UserVO refreshUserLoginData(UserVO user) {
        hiveValidator.validate(user);
        final long loginTimeout = configurationService.getLong(Constants.LAST_LOGIN_TIMEOUT, Constants.LAST_LOGIN_TIMEOUT_DEFAULT);
        return updateStatisticOnSuccessfulLogin(user, loginTimeout);
    }

    protected Optional<UserVO> checkPassword(UserVO user, String password) {
        boolean validPassword = passwordService.checkPassword(password, user.getPasswordSalt(), user.getPasswordHash());

        long loginTimeout = configurationService.getLong(Constants.LAST_LOGIN_TIMEOUT, Constants.LAST_LOGIN_TIMEOUT_DEFAULT);
        boolean mustUpdateLoginStatistic = user.getLoginAttempts() != 0
                || user.getLastLogin() == null
                || timestampService.getTimestamp() - user.getLastLogin().getTime() > loginTimeout;

        if (validPassword && mustUpdateLoginStatistic) {
            UserVO user1 = updateStatisticOnSuccessfulLogin(user, loginTimeout);
            return of(user1);
        } else if (!validPassword) {
            user.setLoginAttempts(user.getLoginAttempts() + 1);
            if (user.getLoginAttempts()
                    >= configurationService.getInt(Constants.MAX_LOGIN_ATTEMPTS, Constants.MAX_LOGIN_ATTEMPTS_DEFAULT)) {
                user.setStatus(UserStatus.LOCKED_OUT);
                logger.info("User with login {} has been locked out after {} login attempts.", user.getLogin(), user.getLoginAttempts());
                user.setLoginAttempts(0);
            }
            userDao.merge(user);
            return empty();
        }
        return of(user);
    }

    private UserVO updateStatisticOnSuccessfulLogin(UserVO user, long loginTimeout) {
        boolean update = false;
        if (user.getLoginAttempts() != 0) {
            update = true;
            user.setLoginAttempts(0);
        }
        if (user.getLastLogin() == null || timestampService.getTimestamp() - user.getLastLogin().getTime() > loginTimeout) {
            update = true;
            user.setLastLogin(timestampService.getDate());
        }
        return update ? userDao.merge(user) : user;
    }

    /**
     * Allows user access to given network
     *
     * @param userId id of user
     * @param networkId id of network
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void assignNetwork(@NotNull long userId, @NotNull long networkId) {
        UserVO existingUser = userDao.find(userId);
        if (existingUser == null) {
            logger.error("Can't assign network with id {}: user {} not found", networkId, userId);
            throw new HiveException(String.format(Messages.USER_NOT_FOUND, userId), NOT_FOUND.getStatusCode());
        }
        NetworkWithUsersAndDevicesVO existingNetwork = networkDao.findWithUsers(networkId).orElse(null);
        if (Objects.isNull(existingNetwork)) {
            throw new HiveException(String.format(Messages.NETWORK_NOT_FOUND, networkId), NOT_FOUND.getStatusCode());
        }

        networkDao.assignToNetwork(existingNetwork, existingUser);
    }

    /**
     * Revokes user access to given network
     *
     * @param userId id of user
     * @param networkId id of network
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void unassignNetwork(@NotNull long userId, @NotNull long networkId) {
        UserVO existingUser = userDao.find(userId);
        if (existingUser == null) {
            logger.error("Can't unassign network with id {}: user {} not found", networkId, userId);
            throw new HiveException(String.format(Messages.USER_NOT_FOUND, userId), NOT_FOUND.getStatusCode());
        }
        NetworkVO existingNetwork = networkDao.find(networkId);
        if (existingNetwork == null) {
            logger.error("Can't unassign user with id {}: network {} not found", userId, networkId);
            throw new HiveException(String.format(Messages.NETWORK_NOT_FOUND, networkId), NOT_FOUND.getStatusCode());
        }
        userDao.unassignNetwork(existingUser, networkId);
    }

}
