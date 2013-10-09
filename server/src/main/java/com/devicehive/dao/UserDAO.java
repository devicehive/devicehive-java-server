package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.*;
import com.devicehive.service.helpers.PasswordProcessor;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


@Stateless
public class UserDAO {

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;
    @Inject
    private PasswordProcessor passwordService;

    /**
     * Search user by login
     *
     * @param login user's login
     * @return User or null, if there is no such user
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public User findByLogin(String login) {
        TypedQuery<User> query = em.createNamedQuery("User.findByName", User.class);
        query.setParameter("login", login);
        CacheHelper.cacheable(query);
        List<User> users = query.getResultList();
        return users.isEmpty() ? null : users.get(0);
    }


    /**
     * @param login        user login ignored, when loginPattern is specified
     * @param loginPattern login pattern (LIKE %VALUE%) user login will be ignored, if not null
     * @param role         User's role ADMIN - 0, CLIENT - 1
     * @param status       ACTIVE - 0 (normal state, user can logon) , LOCKED_OUT - 1 (locked for multiple login failures), DISABLED - 2 , DELETED - 3;
     * @param sortField    either of "login", "loginAttempts", "role", "status", "lastLogin"
     * @param sortOrderAsc Ascending order, if true
     * @param take         like SQL LIMIT
     * @param skip         like SQL OFFSET
     * @return List of User
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<User> getList(String login, String loginPattern, Integer role, Integer status, String sortField,
                              Boolean sortOrderAsc, Integer take, Integer skip) {

        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<User> criteria = criteriaBuilder.createQuery(User.class);
        Root<User> from = criteria.from(User.class);

        List<Predicate> predicates = new ArrayList<>();

        if (loginPattern != null) {
            predicates.add(criteriaBuilder.like(from.<String>get("login"), loginPattern));
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
            take = Constants.DEFAULT_TAKE;
            resultQuery.setMaxResults(take);
        }

        return resultQuery.getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public User findById(Long id) {
        return em.find(User.class, id);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public User findUserWithNetworks(Long id) {
        TypedQuery<User> query = em.createNamedQuery("User.getWithNetworksById", User.class);
        query.setParameter("id", id);
        List<User> users = query.getResultList();
        return users.isEmpty() ? null : users.get(0);

    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public User findUserWithNetworksByLogin(String login) {
        TypedQuery<User> query = em.createNamedQuery("User.getWithNetworks", User.class);
        query.setParameter("login", login);
        List<User> users = query.getResultList();
        return users.isEmpty() ? null : users.get(0);

    }


    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean hasAccessToNetwork(User user, Network network) {
        TypedQuery<Long> query = em.createNamedQuery("User.hasAccessToNetwork", Long.class);
        query.setParameter("user", user);
        query.setParameter("network", network);
        Long count = query.getSingleResult();
        return count != null && count > 0;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean hasAccessToDevice(User user, Device device) {
        TypedQuery<Long> query = em.createNamedQuery("User.hasAccessToDevice", Long.class);
        query.setParameter("user", user);
        query.setParameter("device", device);
        Long count = query.getSingleResult();
        return count != null && count > 0;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean hasAccessToDevice(User user, String deviceId) {
        TypedQuery<Long> query = em.createNamedQuery("User.hasAccessToDeviceByGuid", Long.class);
        query.setParameter("user", user);
        query.setParameter("guid", deviceId);
        Long count = query.getSingleResult();
        return count != null && count > 0;
    }


    public boolean delete(@NotNull long id) {
        Query query = em.createNamedQuery("User.deleteById");
        query.setParameter("id", id);
        return query.executeUpdate() != 0;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public User create(User user){
        em.persist(user);
        return user;
    }

    public void deleteUser(@NotNull long id) {
        User user = this.findById(id);
        user.setStatus(UserStatus.DELETED);
    }
}
