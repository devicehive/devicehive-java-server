package com.devicehive.service;

import com.devicehive.configuration.Constants;
import com.devicehive.dao.NetworkDAO;
import com.devicehive.dao.UserDAO;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import com.devicehive.model.UserRole;
import com.devicehive.model.UserStatus;
import com.devicehive.service.helpers.PasswordProcessor;

import javax.ejb.*;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.constraints.NotNull;
import javax.ws.rs.NotFoundException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

@Stateless
@EJB(beanInterface = UserService.class, name = "UserService")
public class UserService {

    private static final int maxLoginAttempts = 10;

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    @Inject
    private PasswordProcessor passwordService;

    @Inject
    private UserDAO userDAO;

    @Inject
    private NetworkDAO networkDAO;

    /**
     * Tries to authenticate with given credentials
     *
     * @param login
     * @param password
     * @return User object if authentication is successful or null if not
     */

    public User authenticate(String login, String password) {
        User user = userDAO.findActiveByName(login);
        if (user == null){
            return  null;
        }
        if (!passwordService.checkPassword(password, user.getPasswordSalt(), user.getPasswordHash())) {
            user.setLoginAttempts(user.getLoginAttempts() + 1);
            if (user.getLoginAttempts() >= maxLoginAttempts) {
                user.setStatus(UserStatus.LOCKED_OUT);
            }
            return null;
        } else {
            user.setLoginAttempts(0);
            user.setLastLogin(new Timestamp(System.currentTimeMillis()));
            return user;
        }
    }

    public User updatePassword(@NotNull Long id, String password) {

        User u = userDAO.findById(id);

        if (password != null) {
            String salt = passwordService.generateSalt();
            String hash = passwordService.hashPassword(password, salt);
            u.setPasswordHash(hash);
            u.setPasswordSalt(salt);
        }
        if (userDAO.update(id, u)) {
            return u;
        }
        return null;
    }

    public User updateUser(@NotNull Long id, String login, UserRole role, UserStatus status, String password) {

        User u = userDAO.findById(id);

        if (login != null) {
            u.setLogin(login);
        }

        if (role != null) {
            u.setRole(role);
        }

        if (status != null) {
            u.setStatus(status);
        }

        if (password != null) {
            String salt = passwordService.generateSalt();
            String hash = passwordService.hashPassword(password, salt);
            u.setPasswordHash(hash);
            u.setPasswordSalt(salt);
        }

        if (userDAO.update(id, u)) {
            return u;
        }

        return null;
    }

    public void assignNetwork(@NotNull Long userId, @NotNull Long networkId) {
        User existingUser = userDAO.findById(userId);
        if (existingUser == null){
            throw new NotFoundException();
        }
        Network existingNetwork = networkDAO.getByIdWithUsers(networkId);
        if (existingNetwork == null){
            throw new NotFoundException();
        }
        Set<User> usersSet = existingNetwork.getUsers();
        usersSet.add(existingUser);
        existingNetwork.setUsers(usersSet);
        em.merge(existingNetwork);
    }

    public void unassignNetwork(@NotNull Long userId, @NotNull Long networkId) {
        User existingUser = userDAO.findById(userId);
        if (existingUser == null){
            throw new NotFoundException();
        }
        Network existingNetwork = networkDAO.getByIdWithUsers(networkId);
        if (existingNetwork == null){
            throw new NotFoundException();
        }
        existingNetwork.getUsers().remove(existingUser);
        em.merge(existingNetwork);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<User> getList(String login, String loginPattern, Integer role, Integer status, String sortField,
                              Boolean sortOrderAsc, Integer take, Integer skip) {
        return userDAO.getList(login, loginPattern, role, status, sortField, sortOrderAsc, take, skip);
    }

    public User findByLogin(String login) {
        return userDAO.findByLogin(login);
    }

    public User findById(@NotNull Long id) {
        return userDAO.findById(id);
    }

    public User findUserWithNetworks(@NotNull Long id) {
        return userDAO.findUserWithNetworks(id);
    }

    public User findUserWithNetworksByLogin(@NotNull String login) {
        return userDAO.findUserWithNetworksByLogin(login);
    }

    public User createUser(@NotNull String login, @NotNull UserRole role, @NotNull UserStatus status, @NotNull String password) {
        return userDAO.createUser(login, role, status, password);
    }

    public boolean deleteUser(long id){
        return userDAO.deleteUser(id);
    }


}
