package com.devicehive.dao;


import com.devicehive.configuration.Constants;
import com.devicehive.model.AccessKey;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static com.devicehive.model.AccessKey.Queries.Names.*;
import static com.devicehive.model.AccessKey.Queries.Parameters.*;

@Stateless
public class AccessKeyDAO {

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<AccessKey> list(Long userId, String label,
                                String labelPattern, Integer type,
                                String sortField, Boolean sortOrderAsc,
                                Integer take, Integer skip) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<AccessKey> criteria = criteriaBuilder.createQuery(AccessKey.class);
        Root<AccessKey> from = criteria.from(AccessKey.class);
        from.fetch(USER, JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.equal(from.get(USER).get(ID), userId));

        if (labelPattern != null) {
            predicates.add(criteriaBuilder.like(from.<String>get(LABEL), labelPattern));
        } else {
            if (label != null) {
                predicates.add(criteriaBuilder.equal(from.get(LABEL), label));
            }
        }

        if (type != null) {
            predicates.add(criteriaBuilder.equal(from.get(TYPE), type));
        }

        criteria.where(predicates.toArray(new Predicate[predicates.size()]));

        if (sortField != null) {
            if (sortOrderAsc == null || sortOrderAsc) {
                criteria.orderBy(criteriaBuilder.asc(from.get(sortField)));
            } else {
                criteria.orderBy(criteriaBuilder.desc(from.get(sortField)));
            }
        }

        TypedQuery<AccessKey> resultQuery = em.createQuery(criteria);

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
    public AccessKey get(Long userId, Long accessKeyId) {
        TypedQuery<AccessKey> query = em.createNamedQuery(GET_BY_ID, AccessKey.class);
        query.setParameter(USER_ID, userId);
        query.setParameter(ACCESS_KEY_ID, accessKeyId);
        List<AccessKey> resultList = query.getResultList();
        return resultList.isEmpty() ? null : resultList.get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public AccessKey get(Long userId, String label) {
        TypedQuery<AccessKey> query = em.createNamedQuery(GET_BY_USER_AND_LABEL, AccessKey.class);
        query.setParameter(USER_ID, userId);
        query.setParameter(LABEL, label);
        List<AccessKey> resultList = query.getResultList();
        return resultList.isEmpty() ? null : resultList.get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public AccessKey getWithoutUser(Long userId, Long accessKeyId) {
        TypedQuery<AccessKey> query = em.createNamedQuery(GET_BY_ID_SIMPLE, AccessKey.class);
        query.setParameter(USER_ID, userId);
        query.setParameter(ACCESS_KEY_ID, accessKeyId);
        CacheHelper.cacheable(query);
        List<AccessKey> resultList = query.getResultList();
        return resultList.isEmpty() ? null : resultList.get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public AccessKey get(String accessKey) {
        TypedQuery<AccessKey> query = em.createNamedQuery(GET_BY_KEY, AccessKey.class);
        query.setParameter(KEY, accessKey);
        CacheHelper.cacheable(query);
        List<AccessKey> resultList = query.getResultList();
        return resultList.isEmpty() ? null : resultList.get(0);
    }

    public AccessKey insert(AccessKey newAccessKey) {
        em.persist(newAccessKey);
        return newAccessKey;
    }

    public boolean delete(Long userId, Long accessKeyId) {
        Query query = em.createNamedQuery(DELETE_BY_ID_AND_USER);
        query.setParameter(USER_ID, userId);
        query.setParameter(ACCESS_KEY_ID, accessKeyId);
        return query.executeUpdate() > 0;
    }

    public boolean delete(Long accessKeyId) {
        Query query = em.createNamedQuery(DELETE_BY_ID);
        query.setParameter(ACCESS_KEY_ID, accessKeyId);
        return query.executeUpdate() > 0;
    }

    public AccessKey update(@NotNull @Valid AccessKey accessKey) {
        em.merge(accessKey);
        return accessKey;
    }

    public boolean deleteOlderThan(Timestamp timestamp) {
        Query query = em.createNamedQuery(DELETE_OLDER_THAN);
        query.setParameter(EXPIRATION_DATE, timestamp);
        return query.executeUpdate() > 0;
    }

}
