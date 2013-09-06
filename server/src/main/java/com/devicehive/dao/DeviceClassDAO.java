package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.DeviceClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        Root from = criteria.from(DeviceClass.class);

        List<Predicate> predicates = new ArrayList<>();

        if (namePattern != null) {
            predicates.add(criteriaBuilder.like(from.get("name"), namePattern));
        } else {
            if (name != null) {
                predicates.add(criteriaBuilder.equal(from.get("name"), name));
            }
        }
        if (version != null) {
            predicates.add(criteriaBuilder.equal(from.get("version"), version));
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

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceClass get(@NotNull long id) {
        return em.find(DeviceClass.class, id);
    }

    public boolean delete(@NotNull Long id) {
        Query query = em.createNamedQuery("DeviceClass.deleteById");
        query.setParameter("id", id);
        return query.executeUpdate() != 0;

    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceClass getWithEquipment(@NotNull long id) {
        TypedQuery<DeviceClass> tq = em.createNamedQuery("DeviceClass.getWithEquipment", DeviceClass.class);
        tq.setParameter("id", id);
        List<DeviceClass> result = tq.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceClass> getList() {
        return em.createQuery("select dc from DeviceClass dc").getResultList();
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
        TypedQuery<DeviceClass> query = em.createNamedQuery("DeviceClass.findByNameAndVersion", DeviceClass.class);
        query.setParameter("version", version);
        query.setParameter("name", name);
        List<DeviceClass> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceClass> getByIdOrNameOrVersion(Long deviceClassId, String deviceClassName,
                                                    String deviceClassVersion) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<DeviceClass> deviceClassCriteria = criteriaBuilder.createQuery(DeviceClass.class);
        Root fromDeviceClass = deviceClassCriteria.from(DeviceClass.class);
        List<Predicate> deviceClassPredicates = new ArrayList<>();
        if (deviceClassId != null) {
            deviceClassPredicates.add(criteriaBuilder.equal(fromDeviceClass.get("id"), deviceClassId));
        }
        if (deviceClassName != null) {
            deviceClassPredicates.add(criteriaBuilder.equal(fromDeviceClass.get("name"), deviceClassName));
        }
        if (deviceClassVersion != null) {
            deviceClassPredicates.add(criteriaBuilder.equal(fromDeviceClass.get("version"), deviceClassVersion));
        }
        deviceClassCriteria.where(deviceClassPredicates.toArray(new Predicate[deviceClassPredicates.size()]));
        TypedQuery<DeviceClass> deviceClassQuery = em.createQuery(deviceClassCriteria);
        return deviceClassQuery.getResultList();
    }

}
