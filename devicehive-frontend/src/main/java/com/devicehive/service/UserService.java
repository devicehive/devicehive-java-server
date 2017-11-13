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
import com.devicehive.dao.DeviceTypeDao;
import com.devicehive.dao.NetworkDao;
import com.devicehive.dao.UserDao;
import com.devicehive.exceptions.ActionNotAllowedException;
import com.devicehive.exceptions.HiveException;
import com.devicehive.exceptions.IllegalParametersException;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.rpc.ListUserRequest;
import com.devicehive.model.rpc.ListUserResponse;
import com.devicehive.model.updates.UserUpdate;
import com.devicehive.service.configuration.ConfigurationService;
import com.devicehive.service.helpers.PasswordProcessor;
import com.devicehive.service.helpers.ResponseConsumer;
import com.devicehive.service.time.TimestampService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

/**
 * This class serves all requests to database from controller.
 */
@Component
public class UserService extends BaseUserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final String PASSWORD_REGEXP = "^.{6,128}$";

    private final NetworkDao networkDao;
    private final DeviceTypeDao deviceTypeDao;
    private final RpcClient rpcClient;

    private NetworkService networkService;

    @Autowired
    public UserService(PasswordProcessor passwordService,
                       NetworkDao networkDao,
                       DeviceTypeDao deviceTypeDao,
                       UserDao userDao,
                       TimestampService timestampService,
                       ConfigurationService configurationService,
                       HiveValidator hiveValidator,
                       RpcClient rpcClient) {
        super(passwordService, userDao, timestampService, configurationService, hiveValidator);
        this.networkDao = networkDao;
        this.deviceTypeDao = deviceTypeDao;
        this.rpcClient = rpcClient;
    }

    @Autowired
    public void setNetworkService(NetworkService networkService) {
        this.networkService = networkService;
    }

    /**
     * Tries to authenticate with given credentials
     *
     * @return User object if authentication is successful or null if not
     */
    @Transactional(noRollbackFor = ActionNotAllowedException.class)
    public UserVO authenticate(String login, String password) {
        Optional<UserVO> userOpt = userDao.findByName(login);
        if (!userOpt.isPresent()) {
            return null;
        }
        return checkPassword(userOpt.get(), password)
                .orElseThrow(() -> new ActionNotAllowedException(String.format(Messages.INCORRECT_CREDENTIALS, login)));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public UserVO updateUser(@NotNull Long id, UserUpdate userToUpdate, UserVO curUser) {
        UserVO existing = userDao.find(id);

        if (existing == null) {
            logger.error("Can't update user with id {}: user not found", id);
            throw new NoSuchElementException(String.format(Messages.USER_NOT_FOUND, id));
        }

        if (userToUpdate == null) {
            return existing;
        }

        final boolean isClient = UserRole.CLIENT.equals(curUser.getRole());
        if (isClient) {
            if (userToUpdate.getLogin().isPresent() ||
                    userToUpdate.getStatus().isPresent() ||
                    userToUpdate.getRole().isPresent()) {
                logger.error("Can't update user with id {}: users with the 'client' role not allowed to change their " +
                        "login, status or role", id);
                throw new HiveException(Messages.ADMIN_PERMISSIONS_REQUIRED, FORBIDDEN.getStatusCode());
            }
        }

        if (userToUpdate.getLogin().isPresent()) {
            final String newLogin = StringUtils.trim(userToUpdate.getLogin().orElse(null));
            Optional<UserVO> withSuchLogin = userDao.findByName(newLogin);

            if (withSuchLogin.isPresent() && !withSuchLogin.get().getId().equals(id)) {
                throw new ActionNotAllowedException(Messages.DUPLICATE_LOGIN);
            }
            existing.setLogin(newLogin);
        }

        final Optional<String> newPassword = userToUpdate.getPassword();
        if (newPassword.isPresent() && StringUtils.isNotEmpty(newPassword.get())) {
            final String password = newPassword.get();
            if (StringUtils.isEmpty(password) || !password.matches(PASSWORD_REGEXP)) {
                logger.error("Can't update user with id {}: password required", id);
                throw new IllegalParametersException(Messages.PASSWORD_VALIDATION_FAILED);
            }
            String salt = passwordService.generateSalt();
            String hash = passwordService.hashPassword(password, salt);
            existing.setPasswordSalt(salt);
            existing.setPasswordHash(hash);
        }

        if (userToUpdate.getRoleEnum() != null) {
            existing.setRole(userToUpdate.getRoleEnum());
        }

        if (userToUpdate.getStatusEnum() != null) {
            existing.setStatus(userToUpdate.getStatusEnum());
        }

        existing.setData(userToUpdate.getData().orElse(null));
        
        if (userToUpdate.getIntroReviewed().isPresent()) {
            existing.setIntroReviewed(userToUpdate.getIntroReviewed().get());
        }

        hiveValidator.validate(existing);
        return userDao.merge(existing);
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

    /**
     * Allows user access to given device type
     *
     * @param userId id of user
     * @param deviceTypeId id of device type
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void assignDeviceType(@NotNull long userId, @NotNull long deviceTypeId) {
        UserVO existingUser = userDao.find(userId);
        if (existingUser == null) {
            logger.error("Can't assign device type with id {}: user {} not found", deviceTypeId, userId);
            throw new HiveException(String.format(Messages.USER_NOT_FOUND, userId), NOT_FOUND.getStatusCode());
        }
        DeviceTypeWithUsersAndDevicesVO existingDeviceType = deviceTypeDao.findWithUsers(deviceTypeId).orElse(null);
        if (Objects.isNull(existingDeviceType)) {
            throw new HiveException(String.format(Messages.DEVICE_TYPE_NOT_FOUND, deviceTypeId), NOT_FOUND.getStatusCode());
        }

        deviceTypeDao.assignToDeviceType(existingDeviceType, existingUser);
    }

    /**
     * Revokes user access to given device type
     *
     * @param userId id of user
     * @param deviceTypeId id of device type
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void unassignDeviceType(@NotNull long userId, @NotNull long deviceTypeId) {
        UserVO existingUser = userDao.find(userId);
        if (existingUser == null) {
            logger.error("Can't unassign device type with id {}: user {} not found", deviceTypeId, userId);
            throw new HiveException(String.format(Messages.USER_NOT_FOUND, userId), NOT_FOUND.getStatusCode());
        }
        DeviceTypeVO existingDeviceType = deviceTypeDao.find(deviceTypeId);
        if (existingDeviceType == null) {
            logger.error("Can't unassign user with id {}: device type {} not found", userId, deviceTypeId);
            throw new HiveException(String.format(Messages.DEVICE_TYPE_NOT_FOUND, deviceTypeId), NOT_FOUND.getStatusCode());
        }
        userDao.unassignDeviceType(existingUser, deviceTypeId);
    }
    public CompletableFuture<List<UserVO>> list(ListUserRequest request) {
        return list(request.getLogin(), request.getLoginPattern(), request.getRole(), request.getStatus(), request.getSortField(),
                request.getSortOrder(), request.getTake(), request.getSkip());
    }

    public CompletableFuture<List<UserVO>> list(String login, String loginPattern, Integer role, Integer status, String sortField,
            String sortOrder, Integer take, Integer skip) {
        ListUserRequest request = new ListUserRequest();
        request.setLogin(login);
        request.setLoginPattern(loginPattern);
        request.setRole(role);
        request.setStatus(status);
        request.setSortField(sortField);
        request.setSortOrder(sortOrder);
        request.setTake(take);
        request.setSkip(skip);

        CompletableFuture<Response> future = new CompletableFuture<>();

        rpcClient.call(Request
                .newBuilder()
                .withBody(request)
                .build(), new ResponseConsumer(future));

        return future.thenApply(r -> ((ListUserResponse) r.getBody()).getUsers());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public UserVO createUser(@NotNull UserVO user, String password) {
        hiveValidator.validate(user);
        if (user.getId() != null) {
            throw new IllegalParametersException(Messages.ID_NOT_ALLOWED);
        }
        final String userLogin = StringUtils.trim(user.getLogin());
        user.setLogin(userLogin);
        Optional<UserVO> existing = userDao.findByName(user.getLogin());
        if (existing.isPresent()) {
            throw new ActionNotAllowedException(Messages.DUPLICATE_LOGIN);
        }
        if (StringUtils.isNotEmpty(password) && password.matches(PASSWORD_REGEXP)) {
            String salt = passwordService.generateSalt();
            String hash = passwordService.hashPassword(password, salt);
            user.setPasswordSalt(salt);
            user.setPasswordHash(hash);
        } else {
            throw new IllegalParametersException(Messages.PASSWORD_VALIDATION_FAILED);
        }
        user.setLoginAttempts(Constants.INITIAL_LOGIN_ATTEMPTS);
        if (user.getIntroReviewed() == null) {
            user.setIntroReviewed(false);
        }
        userDao.persist(user);
        return user;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public UserWithNetworkVO createUserWithNetwork(UserVO convertTo, String password) {
        hiveValidator.validate(convertTo);
        UserVO createdUser = createUser(convertTo, password);
        NetworkVO createdNetwork = networkService.createOrUpdateNetworkByUser(createdUser);
        UserWithNetworkVO result = UserWithNetworkVO.fromUserVO(createdUser);
        result.getNetworks().add(createdNetwork);
        return result;
    }

    /**
     * Deletes user by id. deletion is cascade
     *
     * @param id user id
     * @return true in case of success, false otherwise
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public boolean deleteUser(long id) {
        int result = userDao.deleteById(id);
        return result > 0;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public boolean hasAccessToDevice(UserVO user, String deviceId) {
        if (!user.isAdmin()) {
            long count = userDao.hasAccessToDevice(user, deviceId);
            return count > 0;
        }
        return true;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public boolean hasAccessToNetwork(UserVO user, NetworkVO network) {
        if (!user.isAdmin()) {
            long count = userDao.hasAccessToNetwork(user, network);
            return count > 0;
        }
        return true;
    }

}
