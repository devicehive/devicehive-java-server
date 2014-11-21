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
import java.util.List;

import static com.devicehive.model.AccessKey.Queries.Names.*;
import static com.devicehive.model.AccessKey.Queries.Parameters.*;

@Stateless
public class AccessKeyDAO {

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<AccessKey> list(Long userId) {
        TypedQuery<AccessKey> query = em.createNamedQuery(GET_BY_USER_ID, AccessKey.class);
        query.setParameter(USER_ID, userId);
        return query.getResultList();
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

}
