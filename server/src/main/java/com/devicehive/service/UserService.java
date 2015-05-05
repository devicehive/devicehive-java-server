package com.devicehive.service;

import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.configuration.PropertiesService;
import com.devicehive.dao.NetworkDAO;
import com.devicehive.dao.UserDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.model.updates.UserUpdate;
import com.devicehive.service.helpers.PasswordProcessor;
import com.devicehive.util.HiveValidator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;

import static javax.ws.rs.core.Response.Status.*;

/**
 * This class serves all requests to database from controller.
 */
@Stateless
@EJB(beanInterface = UserService.class, name = "UserService")
public class UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    @Inject
    private PasswordProcessor passwordService;
    @EJB
    private UserDAO userDAO;
    @EJB
    private NetworkDAO networkDAO;
    @EJB
    private TimestampService timestampService;
    @EJB
    private ConfigurationService configurationService;
    @EJB
    private HiveValidator hiveValidator;
    @EJB
    private IdentityProviderService identityProviderService;
    @EJB
    private PropertiesService propertiesService;

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;


    /**
     * Tries to authenticate with given credentials
     *
     * @return User object if authentication is successful or null if not
     */
    public User authenticate(String login, String password) {
        User user = userDAO.findByLogin(login);
        if (user == null) {
            return null;
        }

        long loginTimeout = configurationService.getLong(Constants.LAST_LOGIN_TIMEOUT, Constants.LAST_LOGIN_TIMEOUT_DEFAULT);

        if (passwordService.checkPassword(password, user.getPasswordSalt(), user.getPasswordHash())) {
            boolean willBeChanged = user.getLoginAttempts() != 0
                || user.getLastLogin() == null
                || System.currentTimeMillis() - user.getLastLogin().getTime() > loginTimeout;

            // No changes to user, successful authentication
            if (!willBeChanged) {
                return user;
            }
        }

        em.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        // repeat whole auth procedure on locked entity
        if (passwordService.checkPassword(password, user.getPasswordSalt(), user.getPasswordHash())) {
            return updateUserOnSuccessfullLogin(user, loginTimeout);
        } else {
            updateUserOnFailedLogin(user);
            throw new HiveException(String.format(Messages.INCORRECT_CREDENTIALS, login), FORBIDDEN.getStatusCode());
        }
    }

    public User findUser(String login, String password) {
        User user = userDAO.findByLogin(login);
        String message;
        if (user == null) {
            LOGGER.error("Can't find user with login {} and password {}", login, password);
            throw new HiveException(Messages.USER_NOT_FOUND, UNAUTHORIZED.getStatusCode());
        } else if (user.getStatus() != UserStatus.ACTIVE) {
            LOGGER.error("User with login {} is not active", login);
            throw new HiveException(Messages.USER_NOT_ACTIVE, UNAUTHORIZED.getStatusCode());
        } else if (passwordService.checkPassword(password, user.getPasswordSalt(), user.getPasswordHash())) {
            long loginTimeout = configurationService.getLong(Constants.LAST_LOGIN_TIMEOUT, Constants.LAST_LOGIN_TIMEOUT_DEFAULT);
            boolean willBeChanged = user.getLoginAttempts() != 0
                    || user.getLastLogin() == null
                    || System.currentTimeMillis() - user.getLastLogin().getTime() > loginTimeout;

            // No changes to user, successful authentication
            if (!willBeChanged) {
                return user;
            }

            em.refresh(user, LockModeType.PESSIMISTIC_WRITE);
            return updateUserOnSuccessfullLogin(user, loginTimeout);
        } else {
            message = String.format(Messages.INCORRECT_CREDENTIALS, login);
        }

        em.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        updateUserOnFailedLogin(user);
        throw new HiveException(message, UNAUTHORIZED.getStatusCode());
    }

    private User updateUserOnSuccessfullLogin(User user, long loginTimeout) {
        if (user.getLoginAttempts() != 0) {
            user.setLoginAttempts(0);
        }
        if (user.getLastLogin() == null || System.currentTimeMillis() - user.getLastLogin().getTime() > loginTimeout) {
            user.setLastLogin(timestampService.getTimestamp());
        }
        return user;
    }

    private void updateUserOnFailedLogin(User user) {
        em.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        user.setLoginAttempts(user.getLoginAttempts() + 1);
        if (user.getLoginAttempts() >=
                configurationService.getInt(Constants.MAX_LOGIN_ATTEMPTS, Constants.MAX_LOGIN_ATTEMPTS_DEFAULT)) {
            user.setStatus(UserStatus.LOCKED_OUT);
            user.setLoginAttempts(0);
        }
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public User updateUser(@NotNull Long id, UserUpdate userToUpdate, UserRole role) {
        User existing = userDAO.findById(id);

        if (existing == null) {
            LOGGER.error("Can't update user with id {}: user not found", id);
            throw new HiveException(Messages.USER_NOT_FOUND, NOT_FOUND.getStatusCode());
        }
        if (userToUpdate == null) {
            return existing;
        }
        if (userToUpdate.getLogin() != null) {
            final String newLogin = StringUtils.trim(userToUpdate.getLogin().getValue());
            final String oldLogin = existing.getLogin();
            User withSuchLogin = userDAO.findByLogin(newLogin);
            if (withSuchLogin != null && !withSuchLogin.getId().equals(id)) {
                throw new HiveException(Messages.DUPLICATE_LOGIN, FORBIDDEN.getStatusCode());
            }
            existing.setLogin(newLogin);

            final String googleLogin = StringUtils.isNotBlank(userToUpdate.getGoogleLogin().getValue()) ?
                    userToUpdate.getGoogleLogin().getValue() : null;
            final String facebookLogin = StringUtils.isNotBlank(userToUpdate.getFacebookLogin().getValue()) ?
                    userToUpdate.getFacebookLogin().getValue() : null;
            final String githubLogin = StringUtils.isNotBlank(userToUpdate.getGithubLogin().getValue()) ?
                    userToUpdate.getGithubLogin().getValue() : null;

            if (googleLogin != null || facebookLogin != null || githubLogin != null) {
                if (userDAO.findByIdentityLogin(oldLogin, googleLogin, facebookLogin, githubLogin) != null) {
                    throw new HiveException(Messages.DUPLICATE_IDENTITY_LOGIN, FORBIDDEN.getStatusCode());
                }
            }
            existing.setGoogleLogin(googleLogin);
            existing.setFacebookLogin(facebookLogin);
            existing.setGithubLogin(githubLogin);
        }
        if (userToUpdate.getPassword() != null) {
            if (userToUpdate.getOldPassword() != null && StringUtils.isNotBlank(userToUpdate.getOldPassword().getValue())) {
                final String hash = passwordService.hashPassword(userToUpdate.getOldPassword().getValue(),
                        existing.getPasswordSalt());
                if (!hash.equals(existing.getPasswordHash())) {
                    LOGGER.error("Can't update user with id {}: incorrect password provided", id);
                    throw new HiveException(Messages.INCORRECT_CREDENTIALS, FORBIDDEN.getStatusCode());
                }
            } else if (role == UserRole.CLIENT) {
                LOGGER.error("Can't update user with id {}: old password required", id);
                throw new HiveException(Messages.OLD_PASSWORD_REQUIRED, FORBIDDEN.getStatusCode());
            }
            if (StringUtils.isEmpty(userToUpdate.getPassword().getValue())) {
                LOGGER.error("Can't update user with id {}: password required", id);
                throw new HiveException(Messages.PASSWORD_REQUIRED, BAD_REQUEST.getStatusCode());
            }
            String salt = passwordService.generateSalt();
            String hash = passwordService.hashPassword(userToUpdate.getPassword().getValue(), salt);
            existing.setPasswordSalt(salt);
            existing.setPasswordHash(hash);
        }
        if (userToUpdate.getStatus() != null || userToUpdate.getRole() != null) {
            if (role != UserRole.ADMIN) {
                LOGGER.error("Can't update user with id {}: users eith the 'client' role are only allowed to change their password", id);
                throw new HiveException(Messages.INVALID_USER_ROLE, FORBIDDEN.getStatusCode());
            } else if (userToUpdate.getRole() != null) {
                existing.setRole(userToUpdate.getRoleEnum());
            } else {
                existing.setStatus(userToUpdate.getStatusEnum());
            }
        }

        hiveValidator.validate(existing);
        return userDAO.update(existing);
    }

    /**
     * Allows user access to given network
     *
     * @param userId    id of user
     * @param networkId id of network
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void assignNetwork(@NotNull long userId, @NotNull long networkId) {
        User existingUser = userDAO.findById(userId);
        if (existingUser == null) {
            LOGGER.error("Can't assign network with id {}: user {} not found", networkId, userId);
            throw new HiveException(Messages.USER_NOT_FOUND, NOT_FOUND.getStatusCode());
        }
        Network existingNetwork = networkDAO.getByIdWithUsers(networkId);
        if (existingNetwork == null) {
            throw new HiveException(String.format(Messages.NETWORK_NOT_FOUND, networkId), NOT_FOUND.getStatusCode());
        }
        Set<User> usersSet = existingNetwork.getUsers();
        usersSet.add(existingUser);
        existingNetwork.setUsers(usersSet);
        networkDAO.merge(existingNetwork);
    }

    /**
     * Revokes user access to given network
     *
     * @param userId    id of user
     * @param networkId id of network
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void unassignNetwork(@NotNull long userId, @NotNull long networkId) {
        User existingUser = userDAO.findById(userId);
        if (existingUser == null) {
            LOGGER.error("Can't unassign network with id {}: user {} not found", networkId, userId);
            throw new HiveException(Messages.USER_NOT_FOUND, NOT_FOUND.getStatusCode());
        }
        Network existingNetwork = networkDAO.getByIdWithUsers(networkId);
        if (existingNetwork != null) {
            existingNetwork.getUsers().remove(existingUser);
            networkDAO.merge(existingNetwork);
        }
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<User> getList(String login, String loginPattern, Integer role, Integer status, String sortField,
                              Boolean sortOrderAsc, Integer take, Integer skip) {
        return userDAO.getList(login, loginPattern, role, status, sortField, sortOrderAsc, take, skip);
    }

    /**
     * Retrieves user by id (no networks fetched in this case)
     *
     * @param id user id
     * @return User model without networks, or null if there is no such user
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public User findById(@NotNull long id) {
        return userDAO.findById(id);
    }

    /**
     * Retrieves user with networks by id, if there is no networks user hass access to networks will be represented by
     * empty set
     *
     * @param id user id
     * @return User model with networks, or null, if there is no such user
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public User findUserWithNetworks(@NotNull long id) {
        return userDAO.findUserWithNetworks(id);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public User createUser(@NotNull User user, String password) {
        if (user.getId() != null) {
            throw new HiveException(Messages.ID_NOT_ALLOWED, BAD_REQUEST.getStatusCode());
        }
        final String userLogin = StringUtils.trim(user.getLogin());
        user.setLogin(userLogin);
        User existing = userDAO.findByLogin(user.getLogin());
        if (existing != null) {
            throw new HiveException(Messages.DUPLICATE_LOGIN,
                                    FORBIDDEN.getStatusCode());
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
            if (userDAO.findByIdentityLogin(userLogin, googleLogin, facebookLogin, githubLogin) != null) {
                throw new HiveException(Messages.DUPLICATE_IDENTITY_LOGIN, FORBIDDEN.getStatusCode());
            }
            user.setGoogleLogin(googleLogin);
            user.setFacebookLogin(facebookLogin);
            user.setGithubLogin(githubLogin);
        }
        user.setLoginAttempts(Constants.INITIAL_LOGIN_ATTEMPTS);

        hiveValidator.validate(user);
        return userDAO.create(user);
    }

    /**
     * Deletes user by id. deletion is cascade
     *
     * @param id user id
     * @return true in case of success, false otherwise
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean deleteUser(long id) {
        return userDAO.delete(id);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean hasAccessToDevice(User user, String deviceGuid) {
        return user.isAdmin() || userDAO.hasAccessToDevice(user, deviceGuid);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean hasAccessToNetwork(User user, Network network) {
        return user.isAdmin() || userDAO.hasAccessToNetwork(user, network);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public User findGoogleUser(String login) {
        return userDAO.findByGoogleLogin(login);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public User findFacebookUser(String login) {
        return userDAO.findByFacebookLogin(login);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public User findGithubUser(String login) {
        return userDAO.findByGithubLogin(login);
    }

    public User refreshUserLoginData(User user) {
        final long loginTimeout = configurationService.getLong(Constants.LAST_LOGIN_TIMEOUT, Constants.LAST_LOGIN_TIMEOUT_DEFAULT);
        final boolean updateLoginAttempts = user.getLoginAttempts() != 0;
        final boolean updateLastLogin = user.getLastLogin() == null || System.currentTimeMillis() - user.getLastLogin().getTime() > loginTimeout;
        if (updateLoginAttempts || updateLastLogin) {
            em.refresh(user, LockModeType.PESSIMISTIC_WRITE);
            if (updateLoginAttempts) {
                user.setLoginAttempts(0);
            }
            if (updateLastLogin) {
                user.setLastLogin(timestampService.getTimestamp());
            }
            hiveValidator.validate(user);
            return userDAO.update(user);
        }
        return user;
    }

}
