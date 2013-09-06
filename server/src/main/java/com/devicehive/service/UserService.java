package com.devicehive.service;

import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.dao.NetworkDAO;
import com.devicehive.dao.UserDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.*;
import com.devicehive.service.helpers.PasswordProcessor;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Set;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

/**
 * This class serves all requests to database from controller.
 */
@Stateless
@EJB(beanInterface = UserService.class, name = "UserService")
public class UserService {

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

    /**
     * updates user, returns null in case of error (for example there is no such user)
     *
     * @param id       user id to update if null field is left unchanged
     * @param login    new login if null field is left unchanged
     * @param role     new role if null field is left unchanged
     * @param status   new status if null field is left unchanged
     * @param password new password if null field is left unchanged
     * @return updated user model
     */
    public User updateUser(@NotNull long id, String login, UserRole role, UserStatus status, String password) {

        User existingUser = userDAO.findById(id);
        if (existingUser == null) {
            throw new HiveException("User not found.", NOT_FOUND.getStatusCode());
        }
        if (login != null) {
            existingUser.setLogin(login);
        }

        if (role != null) {
            existingUser.setRole(role);
        }

        if (status != null) {
            existingUser.setStatus(status);
        }

        if (password != null) {
            String salt = passwordService.generateSalt();
            String hash = passwordService.hashPassword(password, salt);
            existingUser.setPasswordHash(hash);
            existingUser.setPasswordSalt(salt);
        }

        return existingUser;
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

    /**
     * creates new user
     *
     * @param login    user login
     * @param role     user role
     * @param status   user status
     * @param password password
     * @return User model of newly created user
     */
    public User createUser(@NotNull String login, @NotNull UserRole role, @NotNull UserStatus status,
                           @NotNull String password) {
        if (userDAO.findByLogin(login) != null){
            throw new HiveException("User with such login already exists", Response.Status.FORBIDDEN.getStatusCode());
        }
        return userDAO.createUser(login, role, status, password);
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
        return userDAO.hasAccessToDevice(user, device);
    }

}
