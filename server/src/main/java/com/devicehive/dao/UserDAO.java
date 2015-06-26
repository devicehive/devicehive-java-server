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

}
