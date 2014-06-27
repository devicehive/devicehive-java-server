package com.devicehive.dao;


import com.devicehive.configuration.Constants;
import com.devicehive.model.OAuthClient;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

import static com.devicehive.model.OAuthClient.Queries.Names.DELETE_BY_ID;
import static com.devicehive.model.OAuthClient.Queries.Names.GET_BY_NAME;
import static com.devicehive.model.OAuthClient.Queries.Names.GET_BY_OAUTH_ID;
import static com.devicehive.model.OAuthClient.Queries.Names.GET_BY_OAUTH_ID_AND_SECRET;
import static com.devicehive.model.OAuthClient.Queries.Parameters.ID;
import static com.devicehive.model.OAuthClient.Queries.Parameters.NAME;
import static com.devicehive.model.OAuthClient.Queries.Parameters.OAUTH_ID;
import static com.devicehive.model.OAuthClient.Queries.Parameters.SECRET;

@Stateless
public class OAuthClientDAO {

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    public OAuthClient insert(OAuthClient client) {
        em.persist(client);
        return client;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public OAuthClient get(Long id) {
        return em.find(OAuthClient.class, id);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public OAuthClient get(String oauthId) {
        TypedQuery<OAuthClient> query = em.createNamedQuery(GET_BY_OAUTH_ID,
                OAuthClient.class);
        query.setParameter(OAUTH_ID, oauthId);
        CacheHelper.cacheable(query);
        List<OAuthClient> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public OAuthClient get(String id, String secret) {
        TypedQuery<OAuthClient> query = em.createNamedQuery(GET_BY_OAUTH_ID_AND_SECRET, OAuthClient.class);
        query.setParameter(OAUTH_ID, id);
        query.setParameter(SECRET, secret);
        List<OAuthClient> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    public boolean delete(Long id) {
        Query query = em.createNamedQuery(DELETE_BY_ID);
        query.setParameter(ID, id);
        return query.executeUpdate() != 0;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<OAuthClient> list(String name,
                                  String namePattern,
                                  String domain,
                                  String oauthId,
                                  String sortField,
                                  Boolean sortOrderAsc,
                                  Integer take,
                                  Integer skip) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<OAuthClient> criteria = criteriaBuilder.createQuery(OAuthClient.class);
        Root<OAuthClient> from = criteria.from(OAuthClient.class);

        List<Predicate> predicates = new ArrayList<>();
        if (namePattern != null) {
            predicates.add(criteriaBuilder.like(from.<String>get(OAuthClient.NAME_COLUMN), namePattern));
        } else {
            if (name != null) {
                predicates.add(criteriaBuilder.equal(from.get(OAuthClient.NAME_COLUMN), name));
            }
        }

        if (domain != null) {
            predicates.add(criteriaBuilder.equal(from.get(OAuthClient.DOMAIN_COLUMN), domain));
        }
        if (oauthId != null) {
            predicates.add(criteriaBuilder.equal(from.get(OAuthClient.OAUTH_ID_COLUMN), oauthId));
        }
        criteria.where(predicates.toArray(new Predicate[predicates.size()]));

        if (sortField != null) {
            if (sortOrderAsc) {
                criteria.orderBy(criteriaBuilder.asc(from.get(sortField)));
            } else {
                criteria.orderBy(criteriaBuilder.desc(from.get(sortField)));
            }
        }

        TypedQuery<OAuthClient> resultQuery = em.createQuery(criteria);
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
    public OAuthClient getByName(String name) {
        TypedQuery<OAuthClient> query = em.createNamedQuery(GET_BY_NAME, OAuthClient.class);
        query.setParameter(NAME, name);
        List<OAuthClient> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }
}
