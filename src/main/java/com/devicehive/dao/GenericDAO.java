package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

@Repository
public class GenericDAO {
    public static final String CACHEABLE = "org.hibernate.cacheable";
    public static final String RETRIEVE_MODE = "javax.persistence.cache.retrieveMode";
    public static final String STORE_MODE = "javax.persistence.cache.storeMode";

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;


    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public <T extends Serializable> T find(Class<T> entityClass, Object primaryKey) {
        return em.find(entityClass, primaryKey);
    }

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public <T extends Serializable> List<T> findAll(Class<T> entityClass, Optional<CacheConfig> cacheConfig) {
        final CriteriaQuery<T>  cq = em.getCriteriaBuilder().createQuery( entityClass );
        cq.select(cq.from(entityClass));
        final TypedQuery<T> tq = em.createQuery(cq);
        cacheQuery(tq, cacheConfig);
        return tq.getResultList();
    }

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public <T extends Serializable> boolean isExist(Class<T> entityClass, Object primaryKey) {
        final T entity = find(entityClass, primaryKey);
        return entity != null;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public <T extends Serializable> void persist( T entity ){
        em.persist(entity);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public <T extends Serializable> T merge(T entity) {
        return em.merge(entity);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public <T extends Serializable> void refresh(T entity, LockModeType lockModeType) {
        em.refresh(entity, lockModeType);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public <T extends Serializable> void remove(T entity) {
        em.remove(entity);
    }

    public <E, K extends Serializable> E getReference(Class<E> entityClass, K pk){
        return em.getReference(entityClass,pk);
    }

    public <T extends Serializable> TypedQuery<T> createNamedQuery(Class<T> entityClass, String queryName, Optional<CacheConfig> cacheConfig) {
        TypedQuery<T> query = em.createNamedQuery(queryName, entityClass);
        cacheQuery(query, cacheConfig);
        return query;
    }

    public Query createNamedQuery(String queryName, Optional<CacheConfig> cacheConfig) {
        Query query = em.createNamedQuery(queryName);
        cacheQuery(query, cacheConfig);
        return query;
    }

    public void cacheQuery(Query query, Optional<CacheConfig> cacheConfig) {
        if (cacheConfig.isPresent()) {
            query.setHint(CACHEABLE, true);
            query.setHint(RETRIEVE_MODE, cacheConfig.get().getRetrieveMode());
            query.setHint(STORE_MODE, cacheConfig.get().getStoreMode());
        }
    }

    public <T extends Serializable> TypedQuery<T> createQuery(String query, Class<T> entityClass) {
        return em.createQuery(query, entityClass);
    }

    public CriteriaBuilder criteriaBuilder() {
        return em.getCriteriaBuilder();
    }

    public <T extends Serializable> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
        return em.createQuery(criteriaQuery);
    }

}
