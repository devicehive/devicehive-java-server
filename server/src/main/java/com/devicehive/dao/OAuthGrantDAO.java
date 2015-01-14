package com.devicehive.dao;


import com.devicehive.configuration.Constants;
import com.devicehive.model.AccessKey;
import com.devicehive.model.OAuthClient;
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

import static com.devicehive.model.OAuthGrant.Queries.Names.*;
import static com.devicehive.model.OAuthGrant.Queries.Parameters.*;

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
        TypedQuery<OAuthGrant> query = em.createNamedQuery(GET_BY_ID_AND_USER, OAuthGrant.class);
        query.setParameter(GRANT_ID, grantId);
        query.setParameter(USER, user);
        List<OAuthGrant> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public OAuthGrant get(Long grantId) {
        TypedQuery<OAuthGrant> query = em.createNamedQuery(GET_BY_ID, OAuthGrant.class);
        query.setParameter(GRANT_ID, grantId);
        List<OAuthGrant> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    public boolean delete(User user, Long grantId) {
        Query query = em.createNamedQuery(DELETE_BY_USER_AND_ID);
        query.setParameter(GRANT_ID, grantId);
        query.setParameter(USER, user);
        return query.executeUpdate() != 0;
    }

    public boolean delete(Long grantId) {
        Query query = em.createNamedQuery(DELETE_BY_ID);
        query.setParameter(GRANT_ID, grantId);
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
        from.fetch(OAuthGrant.ACCESS_KEY_COLUMN, JoinType.LEFT).fetch(AccessKey.PERMISSIONS_COLUMN);
        from.fetch("client");
        List<Predicate> predicates = new ArrayList<>();

        if (!user.isAdmin()) {
            predicates.add(from.join(OAuthGrant.USER_COLUMN).in(user));
        }

        if (start != null) {
            predicates.add(criteriaBuilder.greaterThan(from.<Timestamp>get(OAuthGrant.TIMESTAMP_COLUMN), start));
        }
        if (end != null) {
            predicates.add(criteriaBuilder.lessThan(from.<Timestamp>get(OAuthGrant.TIMESTAMP_COLUMN), end));
        }
        if (clientOAuthId != null) {
            Predicate oauthIdPredicate =
                criteriaBuilder.equal(
                    from.join(OAuthGrant.OAUTH_CLIENT_COLUMN).get(OAuthClient.OAUTH_ID_COLUMN), clientOAuthId);
            predicates.add(oauthIdPredicate);
        }
        if (type != null) {
            predicates.add(criteriaBuilder.equal(from.get(OAuthGrant.TYPE_COLUMN), type));
        }
        if (accessType != null) {
            predicates.add(criteriaBuilder.equal(from.get(OAuthGrant.ACCESS_TYPE_COLUMN), accessType));
        }
        if (scope != null) {
            predicates.add(criteriaBuilder.equal(from.get(OAuthGrant.SCOPE_COLUMN), scope));
        }
        if (redirectUri != null) {
            predicates.add(criteriaBuilder.equal(from.get(OAuthGrant.REDIRECT_URI_COLUMN), redirectUri));
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
        }
        resultQuery.setMaxResults(take);

        return resultQuery.getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public OAuthGrant getByCodeAndOauthID(String authCode, String oauthId) {
        TypedQuery<OAuthGrant> query = em.createNamedQuery(GET_BY_CODE_AND_OAUTH_ID, OAuthGrant.class);
        query.setParameter(AUTH_CODE, authCode);
        query.setParameter(OAUTH_ID, oauthId);
        List<OAuthGrant> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

}
