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



    @Transactional
    public User findByLogin(String login) {
        TypedQuery<User> query = em.createNamedQuery("User.findByName", User.class);
        query.setParameter("login", login);
        return query.getSingleResult();
    }



    @Transactional
    public User findById(Long id) {
        return em.find(User.class, id);
    }


    @Transactional(value = Transactional.TxType.MANDATORY)
    public User incrementLoginAttempts(User user) {
        em.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        user.setLoginAttempts(user.getLoginAttempts() != null ? user.getLoginAttempts() + 1 : 1);
        if (user.getLoginAttempts() >= maxLoginAttempts) {
            user.setStatus(User.STATUS.LockedOut.ordinal());
        }
        em.persist(user);
        return user;
    }


    @Transactional(value = Transactional.TxType.MANDATORY)
    public User finalizeLogin(User user) {
        em.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        if (user.getStatus() != User.STATUS.Active.ordinal()) {
            return null;
        }
        user.setLoginAttempts(0);
        em.persist(user);
        return user;
    }


}
