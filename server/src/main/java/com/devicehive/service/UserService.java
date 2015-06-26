package com.devicehive.service;

import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.CacheConfig;
import com.devicehive.dao.GenericDAO;
import com.devicehive.dao.UserDAO;
import com.devicehive.exceptions.ActionNotAllowedException;
import com.devicehive.exceptions.HiveException;
import com.devicehive.exceptions.IllegalParametersException;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.model.updates.UserUpdate;
import com.devicehive.service.helpers.PasswordProcessor;
import com.devicehive.service.time.TimestampService;
import com.devicehive.util.HiveValidator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static javax.ws.rs.core.Response.Status.*;

/**
 * This class serves all requests to database from controller.
 */
@Component
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private PasswordProcessor passwordService;
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private GenericDAO genericDAO;
    @Autowired
    private TimestampService timestampService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private HiveValidator hiveValidator;


    /**
     * Tries to authenticate with given credentials
     *
     * @return User object if authentication is successful or null if not
     */
    @Transactional(noRollbackFor = ActionNotAllowedException.class)
    public User authenticate(String login, String password) {
        Optional<User> userOpt = genericDAO.createNamedQuery(User.class, "User.findByName", of(CacheConfig.get()))
                .setParameter("login", login)
                .getResultList()
                .stream().findFirst();
        if (!userOpt.isPresent()) {
            return null;
        }
        return checkPassword(userOpt.get(), password)
                .orElseThrow(() -> new ActionNotAllowedException(String.format(Messages.INCORRECT_CREDENTIALS, login)));
    }

    @Transactional(noRollbackFor = AccessDeniedException.class)
    public User findUser(String login, String password) {
        User user = genericDAO.createNamedQuery(User.class, "User.findByName", empty())
                .setParameter("login", login)
                .getResultList()
                .stream().findFirst().orElse(null);
        if (user == null) {
            logger.error("Can't find user with login {} and password {}", login, password);
            throw new AccessDeniedException(Messages.USER_NOT_FOUND);
        } else if (user.getStatus() != UserStatus.ACTIVE) {
            logger.error("User with login {} is not active", login);
            throw new AccessDeniedException(Messages.USER_NOT_ACTIVE);
        }
        return checkPassword(user, password)
                .orElseThrow(() -> new AccessDeniedException(String.format(Messages.INCORRECT_CREDENTIALS, login)));
    }

    private Optional<User> checkPassword(User user, String password) {
        boolean validPassword = passwordService.checkPassword(password, user.getPasswordSalt(), user.getPasswordHash());

        long loginTimeout = configurationService.getLong(Constants.LAST_LOGIN_TIMEOUT, Constants.LAST_LOGIN_TIMEOUT_DEFAULT);
        boolean mustUpdateLoginStatistic = user.getLoginAttempts() != 0
                || user.getLastLogin() == null
                || System.currentTimeMillis() - user.getLastLogin().getTime() > loginTimeout;

        if (validPassword && mustUpdateLoginStatistic) {
            return of(updateStatisticOnSuccessfulLogin(user, loginTimeout));
        } else if (!validPassword) {
            user.setLoginAttempts(user.getLoginAttempts() + 1);
            if (user.getLoginAttempts() >=
                    configurationService.getInt(Constants.MAX_LOGIN_ATTEMPTS, Constants.MAX_LOGIN_ATTEMPTS_DEFAULT)) {
                user.setStatus(UserStatus.LOCKED_OUT);
                user.setLoginAttempts(0);
            }
            genericDAO.merge(user);
            return empty();
        }
        return of(user);
    }

    private User updateStatisticOnSuccessfulLogin(User user, long loginTimeout) {
        boolean update = false;
        if (user.getLoginAttempts() != 0) {
            update = true;
            user.setLoginAttempts(0);
        }
        if (user.getLastLogin() == null || System.currentTimeMillis() - user.getLastLogin().getTime() > loginTimeout) {
            update = true;
            user.setLastLogin(timestampService.getTimestamp());
        }
        return update ? genericDAO.merge(user) : user;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public User updateUser(@NotNull Long id, UserUpdate userToUpdate, UserRole role) {
        User existing = genericDAO.find(User.class, id);

        if (existing == null) {
            logger.error("Can't update user with id {}: user not found", id);
            throw new NoSuchElementException(Messages.USER_NOT_FOUND);
        }
        if (userToUpdate == null) {
            return existing;
        }
        if (userToUpdate.getLogin() != null) {
            final String newLogin = StringUtils.trim(userToUpdate.getLogin().getValue());
            final String oldLogin = existing.getLogin();
            User withSuchLogin = genericDAO.createNamedQuery(User.class, "User.findByName", empty())
                    .setParameter("login", newLogin)
                    .getResultList()
                    .stream().findFirst().orElse(null);
            if (withSuchLogin != null && !withSuchLogin.getId().equals(id)) {
                throw new ActionNotAllowedException(Messages.DUPLICATE_LOGIN);
            }
            existing.setLogin(newLogin);

            final String googleLogin = StringUtils.isNotBlank(userToUpdate.getGoogleLogin().getValue()) ?
                    userToUpdate.getGoogleLogin().getValue() : null;
            final String facebookLogin = StringUtils.isNotBlank(userToUpdate.getFacebookLogin().getValue()) ?
                    userToUpdate.getFacebookLogin().getValue() : null;
            final String githubLogin = StringUtils.isNotBlank(userToUpdate.getGithubLogin().getValue()) ?
                    userToUpdate.getGithubLogin().getValue() : null;

            if (googleLogin != null || facebookLogin != null || githubLogin != null) {
                Optional<User> userWithSameIdentity = genericDAO.createNamedQuery(User.class, "User.findByIdentityName", of(CacheConfig.bypass()))
                        .setParameter("login", oldLogin)
                        .setParameter("googleLogin", googleLogin)
                        .setParameter("facebookLogin", facebookLogin)
                        .setParameter("githubLogin", githubLogin)
                        .getResultList()
                        .stream().findFirst();
                if (userWithSameIdentity.isPresent()) {
                    throw new ActionNotAllowedException(Messages.DUPLICATE_IDENTITY_LOGIN);
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
                    logger.error("Can't update user with id {}: incorrect password provided", id);
                    throw new ActionNotAllowedException(Messages.INCORRECT_CREDENTIALS);
                }
            } else if (role == UserRole.CLIENT) {
                logger.error("Can't update user with id {}: old password required", id);
                throw new ActionNotAllowedException(Messages.OLD_PASSWORD_REQUIRED);
            }
            if (StringUtils.isEmpty(userToUpdate.getPassword().getValue())) {
                logger.error("Can't update user with id {}: password required", id);
                throw new IllegalParametersException(Messages.PASSWORD_REQUIRED);
            }
            String salt = passwordService.generateSalt();
            String hash = passwordService.hashPassword(userToUpdate.getPassword().getValue(), salt);
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
            existing.setData(userToUpdate.getData().getValue());
        }
        hiveValidator.validate(existing);
        return genericDAO.merge(existing);
    }

    /**
     * Allows user access to given network
     *
     * @param userId    id of user
     * @param networkId id of network
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void assignNetwork(@NotNull long userId, @NotNull long networkId) {
        User existingUser = genericDAO.find(User.class, userId);
        if (existingUser == null) {
            logger.error("Can't assign network with id {}: user {} not found", networkId, userId);
            throw new NoSuchElementException(Messages.USER_NOT_FOUND);
        }
        Network existingNetwork = genericDAO.createNamedQuery(Network.class, "Network.findWithUsers", of(CacheConfig.refresh()))
                .setParameter("id", networkId)
                .getResultList()
                .stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException(String.format(Messages.NETWORK_NOT_FOUND, networkId)));
        Set<User> usersSet = existingNetwork.getUsers();
        usersSet.add(existingUser);
        existingNetwork.setUsers(usersSet);
        genericDAO.merge(existingNetwork);
    }

    /**
     * Revokes user access to given network
     *
     * @param userId    id of user
     * @param networkId id of network
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void unassignNetwork(@NotNull long userId, @NotNull long networkId) {
        User existingUser = genericDAO.find(User.class, userId);
        if (existingUser == null) {
            logger.error("Can't unassign network with id {}: user {} not found", networkId, userId);
            throw new NoSuchElementException(Messages.USER_NOT_FOUND);
        }
        genericDAO.createNamedQuery(Network.class, "Network.findWithUsers", of(CacheConfig.refresh()))
                .setParameter("id", networkId)
                .getResultList()
                .stream().findFirst()
                .ifPresent(existingNetwork -> {
                    existingNetwork.getUsers().remove(existingUser);
                    genericDAO.merge(existingNetwork);
                });
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
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
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public User findById(@NotNull long id) {
        return genericDAO.find(User.class, id);
    }

    /**
     * Retrieves user with networks by id, if there is no networks user hass access to networks will be represented by
     * empty set
     *
     * @param id user id
     * @return User model with networks, or null, if there is no such user
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public User findUserWithNetworks(@NotNull long id) {
        return genericDAO.createNamedQuery(User.class, "User.getWithNetworksById", of(CacheConfig.refresh()))
                .setParameter("id", id)
                .getResultList()
                .stream().findFirst().orElse(null);

    }

    @Transactional(propagation = Propagation.REQUIRED)
    public User createUser(@NotNull User user, String password) {
        if (user.getId() != null) {
            throw new IllegalParametersException(Messages.ID_NOT_ALLOWED);
        }
        final String userLogin = StringUtils.trim(user.getLogin());
        user.setLogin(userLogin);
        Optional<User> existing = genericDAO.createNamedQuery(User.class, "User.findByName", of(CacheConfig.bypass()))
                .setParameter("login", user.getLogin())
                .getResultList()
                .stream().findFirst();
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
            Optional<User> userWithSameIdentity = genericDAO.createNamedQuery(User.class, "User.findByIdentityName", of(CacheConfig.bypass()))
                    .setParameter("login", userLogin)
                    .setParameter("googleLogin", googleLogin)
                    .setParameter("facebookLogin", facebookLogin)
                    .setParameter("githubLogin", githubLogin)
                    .getResultList()
                    .stream().findFirst();
            if (userWithSameIdentity.isPresent()) {
                throw new ActionNotAllowedException(Messages.DUPLICATE_IDENTITY_LOGIN);
            }
            user.setGoogleLogin(googleLogin);
            user.setFacebookLogin(facebookLogin);
            user.setGithubLogin(githubLogin);
        }
        user.setLoginAttempts(Constants.INITIAL_LOGIN_ATTEMPTS);
        hiveValidator.validate(user);
        genericDAO.persist(user);
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
        int result = genericDAO.createNamedQuery("User.deleteById", of(CacheConfig.bypass()))
                .setParameter("id", id)
                .executeUpdate();
        return result > 0;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public boolean hasAccessToDevice(User user, String deviceGuid) {
        if (!user.isAdmin()) {
            long count = genericDAO.createNamedQuery(Long.class, "User.hasAccessToDevice", empty())
                    .setParameter("user", user)
                    .setParameter("guid", deviceGuid)
                    .getSingleResult();
            return count > 0;
        }
        return true;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public boolean hasAccessToNetwork(User user, Network network) {
        if (!user.isAdmin()) {
            long count = genericDAO.createNamedQuery(Long.class, "User.hasAccessToNetwork", empty())
                    .setParameter("user", user)
                    .setParameter("network", network)
                    .getSingleResult();
            return count > 0;
        }
        return true;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public User findGoogleUser(String login) {
        return genericDAO.createNamedQuery(User.class, "User.findByGoogleName", empty())
                .setParameter("login", login)
                .getResultList()
                .stream().findFirst().orElse(null);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public User findFacebookUser(String login) {
        return genericDAO.createNamedQuery(User.class, "User.findByFacebookName", empty())
                .setParameter("login", login)
                .getResultList()
                .stream().findFirst().orElse(null);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public User findGithubUser(String login) {
        return genericDAO.createNamedQuery(User.class, "User.findByGithubName", empty())
                .setParameter("login", login)
                .getResultList()
                .stream().findFirst().orElse(null);
    }

    @Transactional
    public User refreshUserLoginData(User user) {
        hiveValidator.validate(user);
        final long loginTimeout = configurationService.getLong(Constants.LAST_LOGIN_TIMEOUT, Constants.LAST_LOGIN_TIMEOUT_DEFAULT);
        return updateStatisticOnSuccessfulLogin(user, loginTimeout);
    }

}
