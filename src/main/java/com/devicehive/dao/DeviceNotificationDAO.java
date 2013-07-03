package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceNotification;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DeviceNotificationDAO {
    private static final Integer DEFAULT_TAKE = Integer.valueOf(1000);

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    @Transactional
    public void saveNotification(DeviceNotification deviceNotification) {
        try {
            deviceNotification.setTimestamp(new Date(System.currentTimeMillis()));
            em.persist(deviceNotification);
        } catch (Exception e) {
            e.getMessage();
        }
    }

    @Transactional
    public DeviceNotification findById(Long id) {
        return em.find(DeviceNotification.class, id);
    }

    @Transactional
    public List<DeviceNotification> findByDevicesNewerThan(List<Device> deviceList, Date timestamp){
        TypedQuery<DeviceNotification> query = em.createNamedQuery("DeviceNotification.getByDeviceNewerThan",
                DeviceNotification.class);
        query.setParameter("deviceList", deviceList);
        query.setParameter("timestamp", timestamp);
        return query.getResultList();
    }

    @Transactional
    public List<DeviceNotification> findNewerThan(Date timestamp){
        TypedQuery<DeviceNotification> query = em.createNamedQuery("DeviceNotification.getByNewerThan",
                DeviceNotification.class);
        query.setParameter("timestamp", timestamp);
        return query.getResultList();
    }

    @Transactional
    public List<DeviceNotification> queryDeviceCommand(Device device, Date start, Date end, String notification,
                                                       String sortField, Boolean sortOrderAsc, Integer take,
                                                       Integer skip) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<DeviceNotification> criteria = criteriaBuilder.createQuery(DeviceNotification.class);
        Root from = criteria.from(DeviceNotification.class);
        List<Predicate> predicates = new ArrayList<>();

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
            if (sortOrderAsc == null || sortOrderAsc == true) {
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


}
