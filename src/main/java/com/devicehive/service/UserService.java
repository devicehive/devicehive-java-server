package com.devicehive.service;

import com.devicehive.configuration.Constants;
import com.devicehive.dao.NetworkDAO;
import com.devicehive.dao.UserDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import com.devicehive.service.helpers.PasswordProcessor;

import javax.ejb.*;
import javax.inject.Inject;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.List;

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
     * @param login
     * @param password
     * @return User object if authentication is successful or null if not
     */

    public User authenticate(String login, String password) {
        TypedQuery<User> query = em.createNamedQuery("User.findActiveByName", User.class);
        query.setParameter("login", login);
        List<User> list = query.getResultList();

        if (list.isEmpty()) {
            return null;
        }
        User user = list.get(0);
        if (!passwordService.checkPassword(password, user.getPasswordSalt(), user.getPasswordHash())) {
            user.setLoginAttempts(user.getLoginAttempts() + 1);
            if (user.getLoginAttempts() >= maxLoginAttempts) {
                user.setStatus(User.STATUS.LockedOut.ordinal());
            }
            return null;
        } else {
            user.setLoginAttempts(0);
            return user;
        }
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean hasAccessToNetwork(User user, Network network) {
        TypedQuery<Long> query = em.createNamedQuery("User.hasAccessToNetwork", Long.class);
        query.setParameter("user", user);
        query.setParameter("network", network);
        Long count = query.getSingleResult();
        return count != null && count > 0;
    }

    @Lock
    public User createUser(@NotNull String login, @NotNull User.ROLE role, @NotNull User.STATUS status, @NotNull String password) {
        TypedQuery<User> query = em.createNamedQuery("User.findByName", User.class);
        query.setParameter("login", login);
        List<User> list = query.getResultList();
        if (!list.isEmpty()) {
            throw new HiveException("User " + login + " exists");
        }
        User user = new User();
        user.setLogin(login);
        user.setRole(role.ordinal());
        user.setStatus(status.ordinal());
        user.setPasswordSalt(passwordService.generateSalt());
        user.setPasswordHash(passwordService.hashPassword(password, user.getPasswordSalt()));
        user.setLoginAttempts(0);
        return em.merge(user);
    }

    public void updateUser(@NotNull Long id, String login, User.ROLE role, User.STATUS status, String password) {
        User u = em.find(User.class,id);
        if (login != null) {
            u.setLogin(login);
        }
        if (role != null) {
            u.setRole(role.ordinal());
        }
        if (status != null) {
            u.setStatus(status.ordinal());
        }
        if (password != null) {
            String salt = passwordService.generateSalt();
            String hash = passwordService.hashPassword(password, salt);
            u.setPasswordHash(hash);
            u.setPasswordSalt(salt);
        }
        em.merge(u);
    }

    public boolean deleteUser(@NotNull Long id) {
        Query q = em.createNamedQuery("User.delete");
        q.setParameter("id",id);
        return q.executeUpdate() > 0;
    }

    public void assignNetwork(@NotNull Long userId,@NotNull Long networkId) {
        User u = userDAO.findById(userId);
        Network n = networkDAO.getByIdWithUsers(networkId);
        n.getUsers().add(u);
        em.persist(n);
    }

    public void unassignNetwork(@NotNull Long userId,@NotNull Long networkId) {
        User u = userDAO.findById(userId);
        Network n = networkDAO.getByIdWithUsers(networkId);
        n.getUsers().remove(u);
        em.merge(n);
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





}
