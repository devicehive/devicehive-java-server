package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.DeviceClass;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

import static com.devicehive.model.DeviceClass.Queries.Names.*;
import static com.devicehive.model.DeviceClass.Queries.Parameters.*;

/**
 * TODO JavaDoc
 */

@Stateless
public class DeviceClassDAO {

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceClass> getDeviceClassList(String name, String namePattern, String version, String sortField,
                                                Boolean sortOrderAsc, Integer take, Integer skip) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<DeviceClass> criteria = criteriaBuilder.createQuery(DeviceClass.class);
        Root<DeviceClass> from = criteria.from(DeviceClass.class);

        List<Predicate> predicates = new ArrayList<>();

        if (namePattern != null) {
            predicates.add(criteriaBuilder.like(from.<String>get(DeviceClass.NAME_COLUMN), namePattern));
        } else {
            if (name != null) {
                predicates.add(criteriaBuilder.equal(from.get(DeviceClass.NAME_COLUMN), name));
            }
        }
        if (version != null) {
            predicates.add(criteriaBuilder.equal(from.get(DeviceClass.VERSION_COLUMN), version));
        }

        criteria.where(predicates.toArray(new Predicate[predicates.size()]));
        if (sortField != null) {
            if (sortOrderAsc == null || sortOrderAsc) {
                criteria.orderBy(criteriaBuilder.asc(from.get(sortField)));
            } else {
                criteria.orderBy(criteriaBuilder.desc(from.get(sortField)));
            }
        }

        TypedQuery<DeviceClass> resultQuery = em.createQuery(criteria);

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

    public boolean delete(@NotNull Long id) {
        Query query = em.createNamedQuery(DELETE_BY_ID);
        query.setParameter(ID, id);
        return query.executeUpdate() != 0;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceClass getWithEquipment(@NotNull long id) {
        TypedQuery<DeviceClass> tq = em.createNamedQuery(GET_WITH_EQUIPMENT, DeviceClass.class);
        tq.setParameter(ID, id);
        CacheHelper.cacheable(tq);
        List<DeviceClass> result = tq.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceClass> getList() {
        TypedQuery<DeviceClass> query = em.createNamedQuery(GET_ALL, DeviceClass.class);
        CacheHelper.cacheable(query);
        return query.getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceClass getDeviceClass(@NotNull Long id) {
        return em.find(DeviceClass.class, id);
    }

    public DeviceClass createDeviceClass(DeviceClass deviceClass) {
        if (deviceClass.getPermanent() == null) {
            deviceClass.setPermanent(false);
        }
        em.persist(deviceClass);
        return deviceClass;
    }

    /**
     * Updates Device Class
     */
    public DeviceClass updateDeviceClass(DeviceClass deviceClass) {
        return em.merge(deviceClass);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceClass getDeviceClassByNameAndVersion(String name, String version) {
        TypedQuery<DeviceClass> query = em.createNamedQuery(FIND_BY_NAME_AND_VERSION, DeviceClass.class);
        query.setParameter(VERSION, version);
        query.setParameter(NAME, name);
        CacheHelper.cacheable(query);
        List<DeviceClass> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

}
