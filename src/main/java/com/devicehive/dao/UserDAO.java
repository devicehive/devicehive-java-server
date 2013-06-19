package com.devicehive.dao;

import com.devicehive.model.User;
import com.devicehive.service.PasswordService;

import javax.inject.Inject;
import javax.persistence.*;
import javax.transaction.Transactional;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: ssidorenko
 * Date: 18.06.13
 * Time: 11:32
 * To change this template use File | Settings | File Templates.
 */
public class UserDAO {

    private static final int maxLoginAttempts = 10;

    @PersistenceContext(unitName = "devicehive")
    private EntityManager em;

    @Inject
    private PasswordService passwordService;


    @Transactional
    public User findByLogin(String login) {
        TypedQuery<User> query = em.createNamedQuery("User.findByName", User.class);
        query.setParameter("login", login);
        return query.getSingleResult();
    }

    /**
     * Tries to authenticate with given credentials
     * @param login
     * @param password
     * @return User object if authentication is successful or null if not
     */
    // TODO move it to some service class
    @Transactional
    public User authenticate(String login, String password) {
        User user = findByLogin(login);
        if (user == null) {
            return null;
        }
        if (User.STATUS.Active.ordinal() != user.getStatus()) {
            return null;
        }
        if (!passwordService.checkPassword(password, user.getPasswordSalt(), user.getPasswordHash())) {
            incrementLoginAttempts(user);
            return null;
        } else {
            return finalizeLogin(user);
        }
    }

    @Transactional
    public User registerUser(String login, String password) {
        User user = new User();
        user.setLogin(login);
        String salt = passwordService.generateSalt();
        String hash = passwordService.hashPassword(password, salt);
        user.setPasswordSalt(salt);
        user.setPasswordHash(hash);
        user.setStatus(0);
        user.setRole(1);
        user.setLoginAttempts(0);
        em.persist(user);
        return user;
    }



    @Transactional
    public User findById(Long id) {
        return em.find(User.class, id);
    }


    @Transactional(value = Transactional.TxType.MANDATORY)
    protected User incrementLoginAttempts(User user) {
        em.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        user.setLoginAttempts(user.getLoginAttempts() != null ? user.getLoginAttempts() + 1 : 1);
        if (user.getLoginAttempts() >= maxLoginAttempts) {
            user.setStatus(User.STATUS.LockedOut.ordinal());
        }
        em.persist(user);
        return user;
    }


    @Transactional(value = Transactional.TxType.MANDATORY)
    protected User finalizeLogin(User user) {
        em.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        if (user.getStatus() != User.STATUS.Active.ordinal()) {
            return null;
        }
        user.setLoginAttempts(0);
        em.persist(user);
        return user;
    }


}
