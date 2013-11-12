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

@Stateless
public class AccessKeyDAO {

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<AccessKey> list(Long userId){
        TypedQuery<AccessKey> query = em.createNamedQuery("AccessKey.getByUserId", AccessKey.class);
        query.setParameter("userId", userId);
        return query.getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public AccessKey get (Long userId, Long accessKeyId){
        TypedQuery<AccessKey> query = em.createNamedQuery("AccessKey.getById", AccessKey.class);
        query.setParameter("userId", userId);
        query.setParameter("accessKeyId", accessKeyId);
//        CacheHelper.cacheable(query);
        List<AccessKey> resultList = query.getResultList();
        return resultList.isEmpty() ? null : resultList.get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public AccessKey get (String accessKey){
        TypedQuery<AccessKey> query = em.createNamedQuery("AccessKey.getByKey", AccessKey.class);
        query.setParameter("someKey", accessKey);
//        CacheHelper.cacheable(query);
        List<AccessKey> resultList = query.getResultList();
        return resultList.isEmpty() ? null : resultList.get(0);
    }

    public AccessKey insert (AccessKey newAccessKey){
        em.persist(newAccessKey);
        return newAccessKey;
    }

    public boolean delete(Long userId, Long accessKeyId){
        Query query = em.createNamedQuery("AccessKey.deleteByIdAndsUser");
        query.setParameter("userId", userId);
        query.setParameter("accessKeyId", accessKeyId);
        return query.executeUpdate() > 0;
    }

    public boolean delete(Long accessKeyId){
        Query query = em.createNamedQuery("AccessKey.deleteById");
        query.setParameter("accessKeyId", accessKeyId);
        return query.executeUpdate() > 0;
    }

}
