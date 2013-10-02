package com.devicehive.service;

import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.dao.NetworkDAO;
import com.devicehive.dao.UserDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.*;
import com.devicehive.model.updates.UserUpdate;
import com.devicehive.service.helpers.PasswordProcessor;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
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
    private PasswordProcessor passwordService;
    private UserDAO userDAO;
    private NetworkDAO networkDAO;
    private TimestampService timestampService;
    private ConfigurationService configurationService;

    @Inject
    public void setPasswordService(PasswordProcessor passwordService) {
        this.passwordService = passwordService;
    }

    @EJB
    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @EJB
    public void setNetworkDAO(NetworkDAO networkDAO) {
        this.networkDAO = networkDAO;
    }

    @EJB
    public void setTimestampService(TimestampService timestampService) {
        this.timestampService = timestampService;
    }

    @EJB
    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    /**
     * Tries to authenticate with given credentials
     *
     * @param login
     * @param password
     * @return User object if authentication is successful or null if not
     */
    public User authenticate(String login, String password) {
        User user = userDAO.findByLogin(login);
        if (user == null) {
            return null;
        }
        if (!passwordService.checkPassword(password, user.getPasswordSalt(), user.getPasswordHash())) {
            user.setLoginAttempts(user.getLoginAttempts() + 1);
            if (user.getLoginAttempts() >=
                    configurationService.getInt(Constants.MAX_LOGIN_ATTEMPTS, Constants.MAX_LOGIN_ATTEMPTS_DEFALUT)) {
                user.setStatus(UserStatus.LOCKED_OUT);
            }
            return null;
        } else {
            user.setLoginAttempts(0);
            if (user.getLastLogin() == null || System.currentTimeMillis() - user.getLastLogin().getTime() >
                    configurationService.getLong(Constants.LAST_LOGIN_TIMEOUT, Constants.LAST_LOGIN_TIMEOUT_DEFAULT)) {
                user.setLastLogin(timestampService.getTimestamp());
            }
            return user;
        }
    }

    /**
     * updates password for user
     *
     * @param id       user id
     * @param password password
     * @return User with updated parameters
     */
    public User updatePassword(@NotNull long id, String password) {

        User u = userDAO.findById(id);

        if (password != null) {
            String salt = passwordService.generateSalt();
            String hash = passwordService.hashPassword(password, salt);
            u.setPasswordHash(hash);
            u.setPasswordSalt(salt);
        }
        return u;
    }

    public User updateUser(@NotNull Long id, UserUpdate userToUpdate) {
        User existing = userDAO.findById(id);

        if (existing == null) {
            throw new HiveException("User with such id cannot be found", NOT_FOUND.getStatusCode());
        }
        if (userToUpdate == null) {
            return existing;
        }
        if (userToUpdate.getLogin() != null) {
            User existingLogin = userDAO.findByLogin(userToUpdate.getLogin().getValue());
            if (existingLogin != null && !existingLogin.getId().equals(id)) {
                throw new HiveException("User with such login already exists. Please, select another one",
                        FORBIDDEN.getStatusCode());
            }
            existing.setLogin(userToUpdate.getLogin().getValue());
        }
        if (userToUpdate.getPassword() != null) {
            if (userToUpdate.getPassword().getValue() == null || userToUpdate.getPassword().getValue().isEmpty()) {
                throw new HiveException("Password is required!", BAD_REQUEST.getStatusCode());
            }
            String salt = passwordService.generateSalt();
            String hash = passwordService.hashPassword(userToUpdate.getPassword().getValue(), salt);
            existing.setPasswordSalt(salt);
            existing.setPasswordHash(hash);
        }
        if (userToUpdate.getRole() != null) {
            existing.setRole(userToUpdate.getRoleEnum());
        }
        if (userToUpdate.getStatus() != null) {
            existing.setStatus(userToUpdate.getStatusEnum());
        }
        return existing;
    }

    /**
     * Allows user access to given network
     *
     * @param userId    id of user
     * @param networkId id of network
     */
    public void assignNetwork(@NotNull long userId, @NotNull long networkId) {
        User existingUser = userDAO.findById(userId);
        if (existingUser == null) {
            throw new HiveException("User with id : " + userId + " not found.", NOT_FOUND.getStatusCode());
        }
        Network existingNetwork = networkDAO.getByIdWithUsers(networkId);
        if (existingNetwork == null) {
            throw new HiveException("Network with id : " + networkId + " not found.", NOT_FOUND.getStatusCode());
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
    public void unassignNetwork(@NotNull long userId, @NotNull long networkId) {
        User existingUser = userDAO.findById(userId);
        if (existingUser == null) {
            throw new HiveException("User with id : " + userId + " not found.", NOT_FOUND.getStatusCode());
        }
        Network existingNetwork = networkDAO.getByIdWithUsers(networkId);
        if (existingNetwork != null) {
            existingNetwork.getUsers().remove(existingUser);
            networkDAO.merge(existingNetwork);
        }
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<User> getList(String login, String loginPattern, Integer role, Integer status, String sortField,
                              Boolean sortOrderAsc, Integer take, Integer skip) {
        return userDAO.getList(login, loginPattern, role, status, sortField, sortOrderAsc, take, skip);
    }

    /**
     * retrieves user by login
     *
     * @param login user login
     * @return User model, or null if there is no such user
     */
    public User findByLogin(String login) {
        return userDAO.findByLogin(login);
    }

    /**
     * Retrieves user by id (no networks fetched in this case)
     *
     * @param id user id
     * @return User model without networks, or null if there is no such user
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public User findById(@NotNull long id) {
        return userDAO.findById(id);
    }

    /**
     * Retrieves user with networks by id, if there is no networks user hass access to
     * networks will be represented by empty set
     *
     * @param id user id
     * @return User model with networks, or null, if there is no such user
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public User findUserWithNetworks(@NotNull long id) {
        return userDAO.findUserWithNetworks(id);
    }

    /**
     * Retrieves user with networks by id, if there is no networks user hass access to
     * networks will be represented by empty set
     *
     * @param login user login
     * @return User model with networks, or null, if there is no such user
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public User findUserWithNetworksByLogin(@NotNull String login) {
        return userDAO.findUserWithNetworksByLogin(login);
    }

    public User createUser(@NotNull User user, String password) {
        if (user.getId() != null) {
            throw new HiveException("Id cannot be specified for new user",BAD_REQUEST.getStatusCode());
        }
        User existing = userDAO.findByLogin(user.getLogin());
        if (existing != null) {
            throw new HiveException("User with such login already exists. Please, select another one",
                    FORBIDDEN.getStatusCode());
        }
        if (password == null || password.isEmpty()) {
            throw new HiveException("Password is required!", BAD_REQUEST.getStatusCode());
        }
        String salt = passwordService.generateSalt();
        String hash = passwordService.hashPassword(password, salt);
        user.setPasswordSalt(salt);
        user.setPasswordHash(hash);
        user.setLoginAttempts(Constants.MAX_LOGIN_ATTEMPTS_DEFALUT);
        return userDAO.create(user);
    }

    /**
     * Deletes user by id. deletion is cascade
     *
     * @param id user id
     * @return true in case of success, false otherwise
     */
    public boolean deleteUser(long id) {
        return userDAO.delete(id);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean checkPermissions(String deviceId, User currentUser, Device currentDevice) {
        if (currentDevice != null) {
            return deviceId.equals(currentDevice.getGuid());
        } else {
            if (currentUser.getRole().equals(UserRole.CLIENT)) {
                return userDAO.hasAccessToDevice(currentUser, deviceId);
            }
        }
        return true;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean hasAccessToDevice(User user, Device device) {
        return user.isAdmin() || userDAO.hasAccessToDevice(user, device);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean hasAccessToNetwork(User user, Network network) {
        return user.isAdmin() || userDAO.hasAccessToNetwork(user, network);
    }

}
