package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import com.devicehive.service.interceptors.ValidationInterceptor;
import org.hibernate.Hibernate;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.Interceptors;
import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@Stateless
public class UserDAO {

    private static final int maxLoginAttempts = 10;
    private static final Integer DEFAULT_TAKE = 1000; //TODO set parameter

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    /**
     * Search user by login
     *
     * @param login user's login
     * @return User or null, if there is no such user
     */
    public User findByLogin(String login) {
        TypedQuery<User> query = em.createNamedQuery("User.findByName", User.class);
        query.setParameter("login", login);
        List<User> users = query.getResultList();
        return users.isEmpty() ? null : users.get(0);
    }


    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<User> getList(String login, String loginPattern, Integer role, Integer status, String sortField,
                              Boolean sortOrderAsc, Integer take, Integer skip) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<User> criteria = criteriaBuilder.createQuery(User.class);
        Root from = criteria.from(User.class);
        List<Predicate> predicates = new ArrayList<>();
        if (loginPattern != null) {
            predicates.add(criteriaBuilder.like(from.get("login"), loginPattern));
        } else {
            if (login != null) {
                predicates.add(criteriaBuilder.equal(from.get("login"), login));
            }
        }
        if (role != null) {
            predicates.add(criteriaBuilder.equal(from.get("role"), role));
        }
        if (status != null) {
            predicates.add(criteriaBuilder.equal(from.get("status"), status));
        }

        criteria.where(predicates.toArray(new Predicate[predicates.size()]));
        if (sortField != null) {
            if (sortOrderAsc == null || sortOrderAsc) {
                criteria.orderBy(criteriaBuilder.asc(from.get(sortField)));
            } else {
                criteria.orderBy(criteriaBuilder.desc(from.get(sortField)));
            }
        }

        TypedQuery<User> resultQuery = em.createQuery(criteria);
        if (skip != null) {
            resultQuery.setFirstResult(skip);
        }
        if (take == null) {
            take = DEFAULT_TAKE;
            resultQuery.setMaxResults(take);
        }
        return resultQuery.getResultList();
    }

    public User findById(Long id) {
        return em.find(User.class, id);
    }

    public User findUserWithNetworks(Long id) {
        TypedQuery<User> query = em.createNamedQuery("User.getWithNetworks", User.class);
        query.setParameter("id", id);
        List<User> users = query.getResultList();
        return users.isEmpty() ? null : users.get(0);

    }

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public User incrementLoginAttempts(User user) {
        em.refresh(user);
        user.setLoginAttempts(user.getLoginAttempts() != null ? user.getLoginAttempts() + 1 : 1);
        if (user.getLoginAttempts() >= maxLoginAttempts) {
            user.setStatus(User.STATUS.LockedOut.ordinal());
        }
        return em.merge(user);
    }

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public User finalizeLogin(User user) {
        em.refresh(user);
        if (user.getStatus() != User.STATUS.Active.ordinal()) {
            return null;
        }
        user.setLoginAttempts(0);
        return em.merge(user);
    }

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public boolean hasAccessToNetwork(User user, Network network) {
        TypedQuery<Long> query = em.createNamedQuery("User.hasAccessToNetwork", Long.class);
        query.setParameter("user", user);
        query.setParameter("network", network);
        Long count = query.getSingleResult();
        return count != null && count > 0;
    }


}
