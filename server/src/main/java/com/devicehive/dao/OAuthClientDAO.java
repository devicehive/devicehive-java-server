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
        TypedQuery<OAuthClient> query = em.createNamedQuery("OAuthClient.getByOAuthId", OAuthClient.class);
        query.setParameter("oauthId", oauthId);
        CacheHelper.cacheable(query);
        List<OAuthClient> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    public OAuthClient get(String id, String secret) {
        TypedQuery<OAuthClient> query = em.createNamedQuery("OAuthClient.getByOAuthIdAndSecret", OAuthClient.class);
        query.setParameter("oauthId", id);
        query.setParameter("secret", secret);
        List<OAuthClient> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    public boolean delete(Long id) {
        Query query = em.createNamedQuery("OAuthClient.deleteById");
        query.setParameter("id", id);
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
            predicates.add(criteriaBuilder.like(from.<String>get("name"), namePattern));
        } else {
            if (name != null) {
                predicates.add(criteriaBuilder.equal(from.get("name"), name));
            }
        }

        if (domain != null) {
            predicates.add(criteriaBuilder.equal(from.get("domain"), domain));
        }
        if (oauthId != null) {
            predicates.add(criteriaBuilder.equal(from.get("oauthId"), oauthId));
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

    public OAuthClient getByName(String name) {
        TypedQuery<OAuthClient> query = em.createNamedQuery("OAuthClient.getByName", OAuthClient.class);
        query.setParameter("name", name);
        List<OAuthClient> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }
}
