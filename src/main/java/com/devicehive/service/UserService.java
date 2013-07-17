package com.devicehive.service;

import com.devicehive.configuration.Constants;
import com.devicehive.dao.NetworkDAO;
import com.devicehive.dao.UserDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import com.devicehive.service.interceptors.ValidationInterceptor;

import javax.ejb.*;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;
import java.util.List;

@Singleton
@Interceptors(ValidationInterceptor.class)
public class UserService {

    private static final int maxLoginAttempts = 10;

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    @Inject
    private PasswordService passwordService;

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

    public boolean updateUser(@NotNull Long id, String login, User.ROLE role, User.STATUS status, String password) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaUpdate<User> criteria = criteriaBuilder.createCriteriaUpdate(User.class);
        Root from = criteria.from(User.class);
        if (login != null) {
            criteria.set("login", login);
        }
        if (role != null) {
            criteria.set("role", role.ordinal());
        }
        if (status != null) {
            criteria.set("status", status.ordinal());
        }
        if (password != null) {
            String salt = passwordService.generateSalt();
            String hash = passwordService.hashPassword(password, salt);
            criteria.set("passwordHash", hash);
            criteria.set("passwordSalt", salt);
        }
        criteria.where(criteriaBuilder.equal(from.get("id"), id));
        return em.createQuery(criteria).executeUpdate() > 0;
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





}
