package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import com.devicehive.service.helpers.PasswordProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

import static com.devicehive.model.User.Queries.Names.*;
import static com.devicehive.model.User.Queries.Parameters.*;

@Component
public class UserDAO {

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    @Autowired
    private PasswordProcessor passwordService;

    /**
     * Search user by login
     *
     * @param login user's login
     * @return User or null, if there is no such user
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public User findByLogin(String login) {
        TypedQuery<User> query = em.createNamedQuery(FIND_BY_NAME, User.class);
        query.setParameter(LOGIN, login);
        CacheHelper.cacheable(query);
        List<User> users = query.getResultList();
        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * Search user by Google login
     *
     * @param login user's login
     * @return User or null, if there is no such user
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public User findByGoogleLogin(String login) {
        TypedQuery<User> query = em.createNamedQuery(FIND_BY_GOOGLE_NAME, User.class);
        query.setParameter(LOGIN, login);
        CacheHelper.cacheable(query);
        List<User> users = query.getResultList();
        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * Search user by Facebook login
     *
     * @param login user's login
     * @return User or null, if there is no such user
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public User findByFacebookLogin(String login) {
        TypedQuery<User> query = em.createNamedQuery(FIND_BY_FACEBOOK_NAME, User.class);
        query.setParameter(LOGIN, login);
        CacheHelper.cacheable(query);
        List<User> users = query.getResultList();
        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * Search user by Github login
     *
     * @param login user's login
     * @return User or null, if there is no such user
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public User findByGithubLogin(String login) {
        TypedQuery<User> query = em.createNamedQuery(FIND_BY_GITHUB_NAME, User.class);
        query.setParameter(LOGIN, login);
        CacheHelper.cacheable(query);
        List<User> users = query.getResultList();
        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * Search user by one of identity providers login
     *
     * @param googleLogin user's google login
     * @param facebookLogin user's facebook login
     * @param githubLogin user's github login
     * @return User or null, if there is no such user
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public User findByIdentityLogin(String login, String googleLogin, String facebookLogin, String githubLogin) {
        TypedQuery<User> query = em.createNamedQuery(FIND_BY_IDENTITY_NAME, User.class);
        query.setParameter(LOGIN, login);
        query.setParameter(GOOGLE_LOGIN, googleLogin);
        query.setParameter(FACEBOOK_LOGIN, facebookLogin);
        query.setParameter(GITHUB_LOGIN, githubLogin);
        CacheHelper.cacheable(query);
        List<User> users = query.getResultList();
        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * @param login        user login ignored, when loginPattern is specified
     * @param loginPattern login pattern (LIKE %VALUE%) user login will be ignored, if not null
     * @param role         User's role ADMIN - 0, CLIENT - 1
     * @param status       ACTIVE - 0 (normal state, user can logon) , LOCKED_OUT - 1 (locked for multiple login
     *                     failures), DISABLED - 2 , DELETED - 3;
     * @param sortField    either of "login", "loginAttempts", "role", "status", "lastLogin"
     * @param sortOrderAsc Ascending order, if true
     * @param take         like SQL LIMIT
     * @param skip         like SQL OFFSET
     * @return List of User
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<User> getList(String login, String loginPattern, Integer role, Integer status, String sortField,
                              Boolean sortOrderAsc, Integer take, Integer skip) {

        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<User> criteria = criteriaBuilder.createQuery(User.class);
        Root<User> from = criteria.from(User.class);

        List<Predicate> predicates = new ArrayList<>();

        if (loginPattern != null) {
            predicates.add(criteriaBuilder.like(from.<String>get(User.LOGIN_COLUMN), loginPattern));
        } else {
            if (login != null) {
                predicates.add(criteriaBuilder.equal(from.get(User.LOGIN_COLUMN), login));
            }
        }

        if (role != null) {
            predicates.add(criteriaBuilder.equal(from.get(User.ROLE_COLUMN), role));
        }

        if (status != null) {
            predicates.add(criteriaBuilder.equal(from.get(User.STATUS_COLUMN), status));
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
        }
        resultQuery.setMaxResults(take);
        CacheHelper.cacheable(resultQuery);

        return resultQuery.getResultList();
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public User findById(Long id) {
        return em.find(User.class, id);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public User findUserWithNetworks(Long id) {
        TypedQuery<User> query = em.createNamedQuery(GET_WITH_NETWORKS_BY_ID, User.class);
        query.setParameter(ID, id);
        CacheHelper.cacheable(query);
        List<User> users = query.getResultList();
        return users.isEmpty() ? null : users.get(0);

    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public boolean hasAccessToNetwork(User user, Network network) {
        TypedQuery<Long> query = em.createNamedQuery(HAS_ACCESS_TO_NETWORK, Long.class);
        query.setParameter(USER, user);
        query.setParameter(NETWORK, network);
        Long count = query.getSingleResult();
        return count != null && count > 0;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public boolean hasAccessToDevice(User user, String deviceGuid) {
        TypedQuery<Long> query = em.createNamedQuery(HAS_ACCESS_TO_DEVICE, Long.class);
        query.setParameter(USER, user);
        query.setParameter(GUID, deviceGuid);
        Long count = query.getSingleResult();
        return count != null && count > 0;
    }

    @Transactional
    public boolean delete(@NotNull long id) {
        Query query = em.createNamedQuery(DELETE_BY_ID);
        query.setParameter(ID, id);
        return query.executeUpdate() != 0;
    }

    @Transactional
    public User create(User user) {
        em.persist(user);
        return user;
    }

    @Transactional
    public User update(@NotNull @Valid User user) {
        em.merge(user);
        return user;
    }
}
