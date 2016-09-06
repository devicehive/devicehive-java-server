package com.devicehive.service;

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.NetworkDao;
import com.devicehive.dao.UserDao;
import com.devicehive.exceptions.ActionNotAllowedException;
import com.devicehive.exceptions.HiveException;
import com.devicehive.exceptions.IllegalParametersException;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.enums.UserStatus;
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
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.NetworkWithUsersAndDevicesVO;
import com.devicehive.vo.UserVO;
import com.devicehive.vo.UserWithNetworkVO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;

/**
 * This class serves all requests to database from controller.
 */
@Component
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private PasswordProcessor passwordService;
    @Autowired
    private NetworkDao networkDao;
    @Autowired
    private UserDao userDao;
    @Autowired
    private TimestampService timestampService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private HiveValidator hiveValidator;
    @Autowired
    private RpcClient rpcClient;

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

    @Transactional(noRollbackFor = AccessDeniedException.class)
    public UserVO findUser(String login, String password) {
        Optional<UserVO> userOpt = userDao.findByName(login);
        if (!userOpt.isPresent()) {
            logger.error("Can't find user with login {} and password {}", login, password);
            throw new AccessDeniedException(Messages.USER_NOT_FOUND);
        } else if (userOpt.get().getStatus() != UserStatus.ACTIVE) {
            logger.error("User with login {} is not active", login);
            throw new AccessDeniedException(Messages.USER_NOT_ACTIVE);
        }
        return checkPassword(userOpt.get(), password)
                .orElseThrow(() -> new AccessDeniedException(String.format(Messages.INCORRECT_CREDENTIALS, login)));
    }

    private Optional<UserVO> checkPassword(UserVO user, String password) {
        boolean validPassword = passwordService.checkPassword(password, user.getPasswordSalt(), user.getPasswordHash());

        long loginTimeout = configurationService.getLong(Constants.LAST_LOGIN_TIMEOUT, Constants.LAST_LOGIN_TIMEOUT_DEFAULT);
        boolean mustUpdateLoginStatistic = user.getLoginAttempts() != 0
                || user.getLastLogin() == null
                || System.currentTimeMillis() - user.getLastLogin().getTime() > loginTimeout;

        if (validPassword && mustUpdateLoginStatistic) {
            UserVO user1 = updateStatisticOnSuccessfulLogin(user, loginTimeout);
            return of(user1);
        } else if (!validPassword) {
            user.setLoginAttempts(user.getLoginAttempts() + 1);
            if (user.getLoginAttempts() >=
                    configurationService.getInt(Constants.MAX_LOGIN_ATTEMPTS, Constants.MAX_LOGIN_ATTEMPTS_DEFAULT)) {
                user.setStatus(UserStatus.LOCKED_OUT);
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
        if (user.getLastLogin() == null || System.currentTimeMillis() - user.getLastLogin().getTime() > loginTimeout) {
            update = true;
            user.setLastLogin(timestampService.getTimestamp());
        }
        return update ? userDao.merge(user) : user;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public UserVO updateUser(@NotNull Long id, UserUpdate userToUpdate, UserRole role) {
        UserVO existing = userDao.find(id);

        if (existing == null) {
            logger.error("Can't update user with id {}: user not found", id);
            throw new NoSuchElementException(Messages.USER_NOT_FOUND);
        }
        if (userToUpdate == null) {
            return existing;
        }
        if (userToUpdate.getLogin() != null) {
            final String newLogin = StringUtils.trim(userToUpdate.getLogin().orElse(null));
            final String oldLogin = existing.getLogin();
            Optional<UserVO> withSuchLogin = userDao.findByName(newLogin);

            if (withSuchLogin.isPresent() && !withSuchLogin.get().getId().equals(id)) {
                throw new ActionNotAllowedException(Messages.DUPLICATE_LOGIN);
            }
            existing.setLogin(newLogin);

            final String googleLogin = StringUtils.isNotBlank(userToUpdate.getGoogleLogin().orElse(null)) ?
                    userToUpdate.getGoogleLogin().orElse(null) : null;
            final String facebookLogin = StringUtils.isNotBlank(userToUpdate.getFacebookLogin().orElse(null)) ?
                    userToUpdate.getFacebookLogin().orElse(null) : null;
            final String githubLogin = StringUtils.isNotBlank(userToUpdate.getGithubLogin().orElse(null)) ?
                    userToUpdate.getGithubLogin().orElse(null) : null;

            if (googleLogin != null || facebookLogin != null || githubLogin != null) {
                Optional<UserVO> userWithSameIdentity = userDao.findByIdentityName(oldLogin, googleLogin,
                        facebookLogin, githubLogin);
                if (userWithSameIdentity.isPresent()) {
                    throw new ActionNotAllowedException(Messages.DUPLICATE_IDENTITY_LOGIN);
                }
            }
            existing.setGoogleLogin(googleLogin);
            existing.setFacebookLogin(facebookLogin);
            existing.setGithubLogin(githubLogin);
        }
        if (userToUpdate.getPassword() != null) {
            if (userToUpdate.getOldPassword() != null && StringUtils.isNotBlank(userToUpdate.getOldPassword().orElse(null))) {
                final String hash = passwordService.hashPassword(userToUpdate.getOldPassword().orElse(null),
                        existing.getPasswordSalt());
                if (!hash.equals(existing.getPasswordHash())) {
                    logger.error("Can't update user with id {}: incorrect password provided", id);
                    throw new ActionNotAllowedException(Messages.INCORRECT_CREDENTIALS);
                }
            } else if (role == UserRole.CLIENT) {
                logger.error("Can't update user with id {}: old password required", id);
                throw new ActionNotAllowedException(Messages.OLD_PASSWORD_REQUIRED);
            }
            if (StringUtils.isEmpty(userToUpdate.getPassword().orElse(null))) {
                logger.error("Can't update user with id {}: password required", id);
                throw new IllegalParametersException(Messages.PASSWORD_REQUIRED);
            }
            String salt = passwordService.generateSalt();
            String hash = passwordService.hashPassword(userToUpdate.getPassword().orElse(null), salt);
            existing.setPasswordSalt(salt);
            existing.setPasswordHash(hash);
        }
        if (userToUpdate.getStatus() != null || userToUpdate.getRole() != null) {
            if (role != UserRole.ADMIN) {
                logger.error("Can't update user with id {}: users eith the 'client' role are only allowed to change their password", id);
                throw new HiveException(Messages.INVALID_USER_ROLE, FORBIDDEN.getStatusCode());
            } else if (userToUpdate.getRoleEnum() != null) {
                existing.setRole(userToUpdate.getRoleEnum());
            } else {
                existing.setStatus(userToUpdate.getStatusEnum());
            }
        }
        if (userToUpdate.getData() != null) {
            existing.setData(userToUpdate.getData().orElse(null));
        }
        hiveValidator.validate(existing);
        return userDao.merge(existing);
    }

    /**
     * Allows user access to given network
     *
     * @param userId    id of user
     * @param networkId id of network
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void assignNetwork(@NotNull long userId, @NotNull long networkId) {
        UserVO existingUser = userDao.find(userId);
        if (existingUser == null) {
            logger.error("Can't assign network with id {}: user {} not found", networkId, userId);
            throw new NoSuchElementException(Messages.USER_NOT_FOUND);
        }
        NetworkWithUsersAndDevicesVO existingNetwork = networkDao.findWithUsers(networkId)
                .orElseThrow(() -> new NoSuchElementException(String.format(Messages.NETWORK_NOT_FOUND, networkId)));
        networkDao.assignToNetwork(existingNetwork, existingUser);
    }

    /**
     * Revokes user access to given network
     *
     * @param userId    id of user
     * @param networkId id of network
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void unassignNetwork(@NotNull long userId, @NotNull long networkId) {
        UserVO existingUser = userDao.find(userId);
        if (existingUser == null) {
            logger.error("Can't unassign network with id {}: user {} not found", networkId, userId);
            throw new NoSuchElementException(Messages.USER_NOT_FOUND);
        }
        userDao.unassignNetwork(existingUser, networkId);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CompletableFuture<List<UserVO>> list(String login, String loginPattern, Integer role, Integer status, String sortField,
                                                  Boolean sortOrderAsc, Integer take, Integer skip) {
        ListUserRequest request = new ListUserRequest();
        request.setLogin(login);
        request.setLoginPattern(loginPattern);
        request.setRole(role);
        request.setStatus(status);
        request.setSortField(sortField);
        request.setSortOrderAsc(sortOrderAsc);
        request.setTake(take);
        request.setSkip(skip);

        CompletableFuture<Response> future = new CompletableFuture<>();

        rpcClient.call(Request
                .newBuilder()
                .withBody(request)
                .build(), new ResponseConsumer(future));

        return future.thenApply(r -> ((ListUserResponse) r.getBody()).getUsers());
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
     * Retrieves user with networks by id, if there is no networks user hass access to networks will be represented by
     * empty set
     *
     * @param id user id
     * @return User model with networks, or null, if there is no such user
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public UserWithNetworkVO findUserWithNetworks(@NotNull long id) {
        return userDao.getWithNetworksById(id);

    }

    @Transactional(propagation = Propagation.REQUIRED)
    public UserVO createUser(@NotNull UserVO user, String password) {
        if (user.getId() != null) {
            throw new IllegalParametersException(Messages.ID_NOT_ALLOWED);
        }
        final String userLogin = StringUtils.trim(user.getLogin());
        user.setLogin(userLogin);
        Optional<UserVO> existing = userDao.findByName(user.getLogin());
        if (existing.isPresent()) {
            throw new ActionNotAllowedException(Messages.DUPLICATE_LOGIN);
        }
        if (StringUtils.isNoneEmpty(password)) {
            String salt = passwordService.generateSalt();
            String hash = passwordService.hashPassword(password, salt);
            user.setPasswordSalt(salt);
            user.setPasswordHash(hash);
        }
        final String googleLogin = StringUtils.isNotBlank(user.getGoogleLogin()) ? user.getGoogleLogin() : null;
        final String facebookLogin = StringUtils.isNotBlank(user.getFacebookLogin()) ? user.getFacebookLogin() : null;
        final String githubLogin = StringUtils.isNotBlank(user.getGithubLogin()) ? user.getGithubLogin() : null;
        if (googleLogin != null || facebookLogin != null || githubLogin != null) {
            Optional<UserVO> userWithSameIdentity = userDao.findByIdentityName(userLogin, googleLogin,
                    facebookLogin, githubLogin);
            if (userWithSameIdentity.isPresent()) {
                throw new ActionNotAllowedException(Messages.DUPLICATE_IDENTITY_LOGIN);
            }
            user.setGoogleLogin(googleLogin);
            user.setFacebookLogin(facebookLogin);
            user.setGithubLogin(githubLogin);
        }
        user.setLoginAttempts(Constants.INITIAL_LOGIN_ATTEMPTS);
        hiveValidator.validate(user);
        userDao.persist(user);
        return user;
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
    public boolean hasAccessToDevice(UserVO user, String deviceGuid) {
        if (!user.isAdmin()) {
            long count = userDao.hasAccessToDevice(user, deviceGuid);
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

    @Transactional(propagation = Propagation.SUPPORTS)
    public UserVO findGoogleUser(String login) {
        return userDao.findByGoogleName(login);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public UserVO findFacebookUser(String login) {
        return userDao.findByFacebookName(login);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public UserVO findGithubUser(String login) {
        return userDao.findByGithubName(login);
    }

    @Transactional
    public UserVO refreshUserLoginData(UserVO user) {
        hiveValidator.validate(user);
        final long loginTimeout = configurationService.getLong(Constants.LAST_LOGIN_TIMEOUT, Constants.LAST_LOGIN_TIMEOUT_DEFAULT);
        return updateStatisticOnSuccessfulLogin(user, loginTimeout);
    }

}
