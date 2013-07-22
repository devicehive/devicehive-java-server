package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.exceptions.dao.NoSuchRecordException;
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


/**
 * TODO JavaDoc
 */

@Stateless
public class DeviceClassDAO {

    private static int DEFAULT_TAKE = 1000;
    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

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
            take = DEFAULT_TAKE;
        }
        resultQuery.setMaxResults(take);
        return resultQuery.getResultList();
    }

    public DeviceClass get(@NotNull Long id) {
        return em.find(DeviceClass.class, id);
    }

    @Deprecated
    public void delete(@NotNull Long id) {
        DeviceClass dc = em.find(DeviceClass.class, id);
        if (dc == null) {
            throw new NoSuchRecordException("There is no DeviceClass entity with id '" + id + "'");
        }
        try {
            em.remove(dc);
        } catch (Throwable e) {
            throw e;
        }
    }

    /**
     * @param id
     * @param deviceClass
     * @return true if update was executed, false otherwise
     */
    public boolean update(@NotNull Long id, DeviceClass deviceClass) {
        Query query = em.createNamedQuery("DeviceClass.updateDeviceClassById");
        query.setParameter("isPermanent", deviceClass.getPermanent());
        query.setParameter("offlineTimeout", deviceClass.getOfflineTimeout());
        query.setParameter("data", deviceClass.getData());
        query.setParameter("id", id);
        return query.executeUpdate() != 0;
    }

    public DeviceClass getWithEquipment(@NotNull long id) {
        TypedQuery<DeviceClass> tq = em.createNamedQuery("DeviceClass.getWithEquipment", DeviceClass.class);
        tq.setParameter("id", id);
        List<DeviceClass> result = tq.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    public List<DeviceClass> getList() {
        return em.createQuery("select dc from DeviceClass dc").getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceClass getDeviceClass(long id) {
        return em.find(DeviceClass.class, id);
    }

    //check addDeviceClass method
    public DeviceClass createDeviceClass(DeviceClass deviceClass) {
        em.persist(deviceClass);
        return deviceClass;
    }

    public DeviceClass getDeviceClassByNameAndVersion(String name, String version) {
        TypedQuery<DeviceClass> query = em.createNamedQuery("DeviceClass.findByNameAndVersion", DeviceClass.class);
        query.setParameter("version", version);
        query.setParameter("name", name);
        List<DeviceClass> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

}
