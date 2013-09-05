package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.User;
import com.devicehive.utils.LogExecutionTime;

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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Stateless
@LogExecutionTime
public class DeviceNotificationDAO {
    private static final Integer DEFAULT_TAKE = 1000;
    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    public DeviceNotification createNotification(DeviceNotification deviceNotification) {
        em.persist(deviceNotification);
        em.refresh(deviceNotification);
        return deviceNotification;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceNotification findById(@NotNull long id) {
        return em.find(DeviceNotification.class, id);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceNotification> findByDevicesNewerThan(List<Device> deviceList, Timestamp timestamp) {
        TypedQuery<DeviceNotification> query = em.createNamedQuery("DeviceNotification.getByDeviceNewerThan",
                DeviceNotification.class);
        query.setParameter("deviceList", deviceList);
        query.setParameter("timestamp", timestamp);
        return query.getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceNotification> findByDevicesIdsNewerThan(List<String> deviceIds, Timestamp timestamp) {
        TypedQuery<DeviceNotification> query = em.createNamedQuery("DeviceNotification.getByDeviceGuidsNewerThan",
                DeviceNotification.class);
        query.setParameter("guidList", deviceIds);
        query.setParameter("timestamp", timestamp);
        return query.getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceNotification> findNewerThan(Timestamp timestamp) {
        TypedQuery<DeviceNotification> query = em.createNamedQuery("DeviceNotification.getByNewerThan",
                DeviceNotification.class);
        query.setParameter("timestamp", timestamp);
        return query.getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceNotification> getByUserNewerThan(User user, Timestamp timestamp) {
        TypedQuery<DeviceNotification> query = em.createNamedQuery("DeviceNotification.getByUserNewerThan",
                DeviceNotification.class);
        query.setParameter("user", user);
        query.setParameter("timestamp", timestamp);
        return query.getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceNotification> getByUserAndDevicesNewerThan(@NotNull User user, @NotNull List<String> deviceIds,
                                                                 @NotNull Timestamp timestamp) {
        TypedQuery<DeviceNotification> query = em.createNamedQuery("DeviceNotification.getByUserAndDevicesNewerThan",
                DeviceNotification.class);
        query.setParameter("user", user);
        query.setParameter("guidList", deviceIds);
        query.setParameter("timestamp", timestamp);
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceNotification> queryDeviceNotification(Device device, Timestamp start, Timestamp end,
                                                            String notification,
                                                            String sortField, Boolean sortOrderAsc, Integer take,
                                                            Integer skip) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<DeviceNotification> criteria = criteriaBuilder.createQuery(DeviceNotification.class);
        Root from = criteria.from(DeviceNotification.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.equal(from.get("device"), device));
        if (start != null) {
            predicates.add(criteriaBuilder.greaterThan(from.get("timestamp"), start));
        }
        if (end != null) {
            predicates.add(criteriaBuilder.lessThan(from.get("timestamp"), end));
        }
        if (notification != null) {
            predicates.add(criteriaBuilder.equal(from.get("notification"), notification));
        }

        criteria.where(predicates.toArray(new Predicate[predicates.size()]));

        if (sortField != null) {
            if (sortOrderAsc) {
                criteria.orderBy(criteriaBuilder.asc(from.get(sortField)));
            } else {
                criteria.orderBy(criteriaBuilder.desc(from.get(sortField)));
            }
        }

        TypedQuery<DeviceNotification> resultQuery = em.createQuery(criteria);
        if (skip != null) {
            resultQuery.setFirstResult(skip);
        }
        if (take == null) {
            take = DEFAULT_TAKE;
            resultQuery.setMaxResults(take);
        }
        return resultQuery.getResultList();
    }


    public boolean deleteNotification(@NotNull long id) {
        Query query = em.createNamedQuery("DeviceNotification.deleteById");
        query.setParameter("id", id);
        return query.executeUpdate() != 0;
    }

    public int deleteNotificationByFK(@NotNull Device device) {
        Query query = em.createNamedQuery("DeviceNotification.deleteByFK");
        query.setParameter("device", device);
        return query.executeUpdate();
    }


}
