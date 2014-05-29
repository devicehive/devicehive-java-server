package com.devicehive.dao;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Constants;
import com.devicehive.dao.filter.AccessKeyBasedFilterForDevices;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import com.devicehive.util.LogExecutionTime;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.devicehive.model.DeviceNotification.Queries.Names.DELETE_BY_FK;
import static com.devicehive.model.DeviceNotification.Queries.Names.DELETE_BY_ID;
import static com.devicehive.model.DeviceNotification.Queries.Parameters.DEVICE;
import static com.devicehive.model.DeviceNotification.Queries.Parameters.ID;

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
    public List<DeviceNotification> findNotifications(Collection<Device> devices, Collection<String> names,
                                                      @NotNull Timestamp timestamp, HivePrincipal principal) {
        if (devices != null && devices.isEmpty())
            return Collections.<DeviceNotification>emptyList();
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<DeviceNotification> criteria = criteriaBuilder.createQuery(DeviceNotification.class);
        Root<DeviceNotification> from = criteria.from(DeviceNotification.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates
                .add(criteriaBuilder.greaterThan(from.<Timestamp>get(DeviceNotification.TIMESTAMP_COLUMN), timestamp));
        if (names != null) {
            predicates.add(from.get(DeviceNotification.NOTIFICATION_COLUMN).in(names));
        }
        if (devices != null) {
            predicates.add(from.join(DeviceNotification.DEVICE_COLUMN).in(devices));
        }
        appendPrincipalPredicates(predicates, principal, from);
        criteria.where(predicates.toArray(new Predicate[predicates.size()]));
        return em.createQuery(criteria).getResultList();
    }

    /*
     If grid interval is present query must looks like this:

     select *
     from device_notification
     inner join (
          select min(device_notification.timestamp) as timestamp from device_notification
          where device_notification.timestamp between '2013-04-14 14:23:00.775+04' and '2014-05-14 14:23:00.775+04'
                and device_notification.device_id = 10732
                and device_notification.notification = 'notificationFromDevice'
          group by (floor((extract(EPOCH FROM device_notification.timestamp)) / 30))) n3
     on device_notification.timestamp = n3.timestamp
     where device_notification.device_id = 10732
     order by device_notification.id;

     or like this, if grid interval is not present

     select * from device_notification
     where device_notification.timestamp between '2013-04-14 14:23:00.775+04' and '2014-04-14 14:23:00.775+04'
     and  device_id = 8038
     and  notification = 'equipment'

     Order, take and skip parameters will be appended to the end of each query if any of them are present.

     The building of this query contain to stages to avoid sql injection:
     1) Creation query as a string with the wildcards as a parameters. The list of the parameters will be made
     synchronously with the adding of the wildcards.
     2) Query parameters are set with query.setParameter(int position, Object value) to avoid sql injection.
     */
    @SuppressWarnings("unchecked")
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceNotification> queryDeviceNotification(Long deviceId,
                                                            Timestamp start,
                                                            Timestamp end,
                                                            String notification,
                                                            String sortField,
                                                            Boolean sortOrderAsc,
                                                            Integer take,
                                                            Integer skip,
                                                            Integer gridInterval) {
        List<Object> parameters = new ArrayList();
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT device_notification.* FROM device_notification");     //this part of query is immutable
        if (gridInterval != null) {
            sb.append(" INNER JOIN ( ")
                    .append("  SELECT min(device_notification.timestamp) AS timestamp FROM device_notification  ");
        }
        if (start != null || end != null || notification != null || deviceId != null) {
            sb.append(" WHERE ");
        }
        if (deviceId != null) {      //device id is required
            sb.append(" device_notification.device_id = ? ");
            parameters.add(deviceId);
        }
        if (start != null && end != null) {
            sb.append(" AND device_notification.timestamp BETWEEN ? AND ? ");
            parameters.add(start);
            parameters.add(end);
        } else if (start != null) {
            sb.append(" AND device_notification.timestamp >= ? ");
            parameters.add(start);
        } else if (end != null) {
            sb.append(" AND device_notification.timestamp <= ? ");
            parameters.add(end);
        }
        if (notification != null) {
            sb.append(" AND device_notification.notification = ? ");
            parameters.add(notification);
        }

        if (gridInterval != null) {
            //select min(timestamp),
            // group by is required. Selection should contain first timestamp in the interval.
            sb.append(" GROUP BY (floor((extract(EPOCH FROM device_notification.timestamp)) / ?))) n3 ");
            parameters.add(gridInterval);
            sb.append("  ON device_notification.timestamp = n3.timestamp ");
            sb.append("  WHERE device_notification.device_id = ? ");
            parameters.add(deviceId);
        }
        if (sortField != null) {
            sb.append(" ORDER BY ").append(sortField);
            if (sortOrderAsc) {
                sb.append(" ASC ");
            } else {
                sb.append(" DESC ");
            }
        }
        if (take != null) {
            sb.append(" LIMIT ").append(take);
        }
        if (skip != null) {
            sb.append(" OFFSET ").append(skip);
        }
        sb.append(";");
        Query query = em.createNativeQuery(sb.toString(), DeviceNotification.class);
        for (int i = 0; i < parameters.size(); i++) {
            query.setParameter(i + 1, parameters.get(i));
        }
        return query.getResultList();
    }

    public boolean deleteNotification(@NotNull long id) {
        Query query = em.createNamedQuery(DELETE_BY_ID);
        query.setParameter(ID, id);
        return query.executeUpdate() != 0;
    }

    public int deleteNotificationByFK(@NotNull Device device) {
        Query query = em.createNamedQuery(DELETE_BY_FK);
        query.setParameter(DEVICE, device);
        return query.executeUpdate();
    }

    private void appendPrincipalPredicates(List<Predicate> predicates, HivePrincipal principal,
                                           Root<DeviceNotification> from) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        if (principal != null) {
            User user = principal.getUser();
            if (user == null && principal.getKey() != null) {
                user = principal.getKey().getUser();
            }
            if (user != null && !user.isAdmin()) {
                Predicate userPredicate = from.join(DeviceNotification.DEVICE_COLUMN)
                        .join(Device.NETWORK_COLUMN).join(Network.USERS_ASSOCIATION).in(user);
                predicates.add(userPredicate);
            }
            if (principal.getDevice() != null) {
                Predicate devicePredicate = from.join(DeviceNotification.DEVICE_COLUMN)
                        .get(DeviceNotification.ID_COLUMN)
                        .in(principal.getDevice().getId());
                predicates.add(devicePredicate);
            }
            if (principal.getKey() != null) {

                List<Predicate> extraPredicates = new ArrayList<>();
                for (AccessKeyBasedFilterForDevices extraFilter : AccessKeyBasedFilterForDevices
                        .createExtraFilters(principal.getKey().getPermissions())) {
                    List<Predicate> filter = new ArrayList<>();
                    if (extraFilter.getDeviceGuids() != null) {
                        filter.add(
                                from.join(DeviceNotification.DEVICE_COLUMN).get(Device.GUID_COLUMN)
                                        .in(extraFilter.getDeviceGuids()
                                        ));
                    }
                    if (extraFilter.getNetworkIds() != null) {
                        Predicate networkFilter =
                                from.join(DeviceNotification.DEVICE_COLUMN)
                                        .join(Device.NETWORK_COLUMN)
                                        .get(Network.ID_COLUMN).in(extraFilter.getNetworkIds());
                        filter.add(networkFilter);
                    }
                    extraPredicates.add(criteriaBuilder.and(filter.toArray(new Predicate[0])));
                }
                predicates.add(criteriaBuilder.or(extraPredicates.toArray(new Predicate[0])));
            }
        }
    }
}
