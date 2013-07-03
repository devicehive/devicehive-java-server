package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.Network;
import com.devicehive.model.User;

import javax.persistence.*;
import javax.transaction.Transactional;


public class UserDAO {

    private static final int maxLoginAttempts = 10;

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;


    @PersistenceContext(unitName = Constants.EMBEDDED_PERSISTENCE_UNIT)
    private EntityManager em2;



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
        em.merge(user);
        return user;
    }


    @Transactional(value = Transactional.TxType.MANDATORY)
    public User finalizeLogin(User user) {
        em.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        if (user.getStatus() != User.STATUS.Active.ordinal()) {
            return null;
        }
        user.setLoginAttempts(0);
        em.merge(user);
        return user;
    }


    @Transactional(value = Transactional.TxType.MANDATORY)
    public boolean hasAccessToNetwork(User user, Network network) {
        TypedQuery<Long> query = em.createNamedQuery("User.hasAccessToNetwork", Long.class);
        query.setParameter("user", user);
        query.setParameter("network", network);
        Long count = query.getSingleResult();
        return count != null && count > 0;
    }


}
