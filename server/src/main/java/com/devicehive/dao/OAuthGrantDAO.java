package com.devicehive.dao;


import com.devicehive.configuration.Constants;
import com.devicehive.model.OAuthGrant;
import com.devicehive.model.User;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Stateless
public class OAuthGrantDAO {

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    public OAuthGrant insert(OAuthGrant toInsert) {
        em.persist(toInsert);
        return toInsert;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public OAuthGrant get(User user, Long grantId) {
        TypedQuery<OAuthGrant> query = em.createNamedQuery("OAuthGrant.getByIdAndUser", OAuthGrant.class);
        query.setParameter("grantId", grantId);
        query.setParameter("user", user);
        List<OAuthGrant> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public OAuthGrant get(Long grantId) {
        TypedQuery<OAuthGrant> query = em.createNamedQuery("OAuthGrant.getById", OAuthGrant.class);
        query.setParameter("grantId", grantId);
        List<OAuthGrant> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }


    public boolean delete(User user, Long grantId){
        Query query = em.createNamedQuery("OAuthGrant.deleteByUserAndId");
        query.setParameter("grantId", grantId);
        query.setParameter("user", user);
        return query.executeUpdate() != 0;
    }


    public boolean delete(Long grantId){
        Query query = em.createNamedQuery("OAuthGrant.deleteById");
        query.setParameter("grantId", grantId);
        return query.executeUpdate() != 0;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<OAuthGrant> get(User user,
                                Timestamp start,
                                Timestamp end,
                                String clientOAuthId,
                                Integer type,
                                String scope,
                                String redirectUri,
                                Integer accessType,
                                String sortField,
                                Boolean sortOrder,
                                Integer take,
                                Integer skip) {

        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<OAuthGrant> criteria = criteriaBuilder.createQuery(OAuthGrant.class);
        Root<OAuthGrant> from = criteria.from(OAuthGrant.class);
        from.fetch("accessKey", JoinType.LEFT).fetch("permissions");
        from.fetch("client");
        List<Predicate> predicates = new ArrayList<>();

        if (!user.isAdmin()) {
            predicates.add(from.join("user").in(user));
        }

        if (start != null) {
            predicates.add(criteriaBuilder.greaterThan(from.<Timestamp>get("timestamp"), start));
        }
        if (end != null) {
            predicates.add(criteriaBuilder.lessThan(from.<Timestamp>get("timestamp"), end));
        }
        if (clientOAuthId != null) {
            predicates.add(criteriaBuilder.equal(from.join("client").get("oauthId"), clientOAuthId));
        }
        if (type != null) {
            predicates.add(criteriaBuilder.equal(from.get("type"), type));
        }
        if (accessType != null) {
            predicates.add(criteriaBuilder.equal(from.get("accessType"), accessType));
        }
        if (scope != null) {
            predicates.add(criteriaBuilder.equal(from.get("scope"), scope));
        }
        if (redirectUri != null) {
            predicates.add(criteriaBuilder.equal(from.get("redirectUri"), redirectUri));
        }
        criteria.where(predicates.toArray(new Predicate[predicates.size()]));
        if (sortField != null) {
            if (sortOrder == null || sortOrder) {
                criteria.orderBy(criteriaBuilder.asc(from.get(sortField.toLowerCase())));
            } else {
                criteria.orderBy(criteriaBuilder.desc(from.get(sortField)));
            }
        }
        TypedQuery<OAuthGrant> resultQuery = em.createQuery(criteria);

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
    public OAuthGrant getByCodeAndOauthID(String authCode, String oauthId) {
        TypedQuery<OAuthGrant> query = em.createNamedQuery("OAuthGrant.getByCodeAndOAuthID", OAuthGrant.class);
        query.setParameter("authCode", authCode);
        query.setParameter("oauthId", oauthId);
        List<OAuthGrant> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

}
