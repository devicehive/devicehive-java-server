package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.DeviceClass;

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
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

import static com.devicehive.model.DeviceClass.Queries.Names.DELETE_BY_ID;
import static com.devicehive.model.DeviceClass.Queries.Names.FIND_BY_NAME_AND_VERSION;
import static com.devicehive.model.DeviceClass.Queries.Names.GET_ALL;
import static com.devicehive.model.DeviceClass.Queries.Names.GET_WITH_EQUIPMENT;
import static com.devicehive.model.DeviceClass.Queries.Parameters.ID;
import static com.devicehive.model.DeviceClass.Queries.Parameters.NAME;
import static com.devicehive.model.DeviceClass.Queries.Parameters.VERSION;

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
        List<DeviceClass> result = tq.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceClass> getList() {
        return em.createNamedQuery(GET_ALL, DeviceClass.class).getResultList();
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
     *
     * @param deviceClass
     * @return
     */
    public DeviceClass updateDeviceClass(DeviceClass deviceClass) {
        return em.merge(deviceClass);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceClass getDeviceClassByNameAndVersion(String name, String version) {
        TypedQuery<DeviceClass> query = em.createNamedQuery(FIND_BY_NAME_AND_VERSION, DeviceClass.class);
        query.setParameter(VERSION, version);
        query.setParameter(NAME, name);
        List<DeviceClass> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

}
