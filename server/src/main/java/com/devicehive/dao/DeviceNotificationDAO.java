package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.User;
import com.devicehive.util.LogExecutionTime;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Stateless
@LogExecutionTime
public class DeviceNotificationDAO {
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
    public List<DeviceNotification> findNotificationsForPolling(@NotNull Timestamp timestamp,
                                                                List<Device> devices,
                                                                User user) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<DeviceNotification> criteria = criteriaBuilder.createQuery(DeviceNotification.class);
        Root<DeviceNotification> from = criteria.from(DeviceNotification.class);
        List<Predicate> predicates = new ArrayList<>();

        if (devices != null && !devices.isEmpty()) {
            predicates.add(from.get("device").in(devices));
        }

        predicates.add(criteriaBuilder.greaterThan(from.<Timestamp>get("timestamp"), timestamp));

        if (user != null && !user.isAdmin()){
            Path<User> path = from.join("device").join("network").join("users");
            predicates.add(path.in(user));
        }

        criteria.where(predicates.toArray(new Predicate[predicates.size()]));
        return em.createQuery(criteria).getResultList();
    }

    @SuppressWarnings("unchecked")
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceNotification> queryDeviceNotification(Device device, Timestamp start, Timestamp end,
                                                            String notification,
                                                            String sortField, Boolean sortOrderAsc, Integer take,
                                                            Integer skip) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<DeviceNotification> criteria = criteriaBuilder.createQuery(DeviceNotification.class);
        Root<DeviceNotification> from = criteria.from(DeviceNotification.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.equal(from.get("device"), device));
        if (start != null) {
            predicates.add(criteriaBuilder.greaterThan(from.<Timestamp>get("timestamp"), start));
        }
        if (end != null) {
            predicates.add(criteriaBuilder.lessThan(from.<Timestamp>get("timestamp"), end));
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
            take = Constants.DEFAULT_TAKE;
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
