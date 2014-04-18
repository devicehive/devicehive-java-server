package com.devicehive.dao;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Constants;
import com.devicehive.dao.filter.AccessKeyBasedFilterForDevices;
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
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public List<DeviceNotification> findNotifications(Map<Device, Set<String>> deviceNamesFilters,
                                                      @NotNull Timestamp timestamp, HivePrincipal principal) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<DeviceNotification> criteria = criteriaBuilder.createQuery(DeviceNotification.class);
        Root<DeviceNotification> from = criteria.from(DeviceNotification.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.greaterThan(from.<Timestamp>get("timestamp"), timestamp));
        appendPrincipalPredicates(predicates, principal, from);
        if (deviceNamesFilters != null && !deviceNamesFilters.isEmpty()) {
            List<Predicate> filterPredicates = new ArrayList<>();
            for (Map.Entry<Device, Set<String>> entry : deviceNamesFilters.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty())
                    filterPredicates.add(
                            criteriaBuilder.and(criteriaBuilder.equal(from.get("device"), entry.getKey()),
                                    from.get("notification").in(entry.getValue())));
                else if (entry.getValue() == null)
                    filterPredicates.add(criteriaBuilder.equal(from.get("device"), entry.getKey()));

            }
            predicates.add(criteriaBuilder.or(filterPredicates.toArray(new Predicate[filterPredicates.size()])));
        }
        criteria.where(predicates.toArray(new Predicate[predicates.size()]));
        return em.createQuery(criteria).getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceNotification> findNotifications(@NotNull Timestamp timestamp, Set<String> names,
                                                      HivePrincipal principal) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<DeviceNotification> criteria = criteriaBuilder.createQuery(DeviceNotification.class);
        Root<DeviceNotification> from = criteria.from(DeviceNotification.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.greaterThan(from.<Timestamp>get("timestamp"), timestamp));
        if (names != null) {
            predicates.add(from.get("notification").in(names));
        }
        appendPrincipalPredicates(predicates, principal, from);
        criteria.where(predicates.toArray(new Predicate[predicates.size()]));
        return em.createQuery(criteria).getResultList();
    }

    /*
     If grid interval is present query must looks like this:

     select * from device_notification
        where device_notification.timestamp in
	 (select min(rank_selection.timestamp)
	 from
		(select device_notification.*,
		       rank() over (partition by device_notification.notification order by floor((extract(EPOCH FROM device_notification.timestamp)) / 30)) as rank
			from device_notification
			where device_notification.timestamp between '2013-04-14 14:23:00.775+04' and '2014-04-14 14:23:00.775+04'
		) as rank_selection
	 where rank_selection.device_id = 8038
       and rank_selection.notification = 'equipment'
	 group by rank_selection.rank, rank_selection.notification);

     If gridInterval is null the query must looks like this:

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
    public List<DeviceNotification> queryDeviceNotification(Device device,
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
        sb.append("SELECT * FROM device_notification ");     //this part of query is immutable
        if (gridInterval != null) {
            sb.append("WHERE device_notification.timestamp IN ")
                    .append("  (SELECT min(rank_selection.timestamp) ")
                    .append("  FROM ")
                    .append("     (SELECT device_notification.*, ")
                    .append("           rank() OVER (PARTITION BY device_notification.notification ORDER BY floor(" +
                            "(extract(EPOCH FROM device_notification.timestamp)) / ?)) AS rank ")
                    .append("      FROM device_notification ");
            parameters.add(gridInterval);
        }
        if (start != null && end != null) {
            sb.append(" WHERE device_notification.timestamp BETWEEN ? AND ? ");
            parameters.add(start);
            parameters.add(end);
        } else if (start != null) {
            sb.append(" WHERE device_notification.timestamp >= ? ");
            parameters.add(start);
        } else if (end != null) {
            sb.append(" WHERE device_notification.timestamp <= ? ");
            parameters.add(end);
        }
        if (gridInterval != null) {
            sb.append(" ) AS rank_selection ");
            sb.append("  WHERE (rank_selection.device_id = ?) ");
        } else {
            if (start != null || end != null)
                sb.append(" AND ");
            else {
                sb.append(" WHERE ");
            }
            sb.append(" device_notification.device_id = ? ");
        }
        parameters.add(device.getId());   //device id is required
        if (notification != null) {
            sb.append(" AND ");
            if (gridInterval != null) {
                sb.append(" (rank_selection.notification = ?) ");
            } else {
                sb.append(" device_notification.notification = ? ");
            }
            parameters.add(notification);
        }
        if (gridInterval != null) {
            sb.append("  GROUP BY rank_selection.rank, rank_selection.notification) ");   //select min(timestamp),
            // group by is required. Selection should contain first timestamp in the interval. Rank is stands for
            // timestamp in seconds / interval length
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
        Query query = em.createNamedQuery("DeviceNotification.deleteById");
        query.setParameter("id", id);
        return query.executeUpdate() != 0;
    }

    public int deleteNotificationByFK(@NotNull Device device) {
        Query query = em.createNamedQuery("DeviceNotification.deleteByFK");
        query.setParameter("device", device);
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
                predicates.add(from.join("device").join("network").join("users").in(user));
            }
            if (principal.getDevice() != null) {
                predicates.add(from.join("device").get("id").in(principal.getDevice().getId()));
            }
            if (principal.getKey() != null) {

                List<Predicate> extraPredicates = new ArrayList<>();
                for (AccessKeyBasedFilterForDevices extraFilter : AccessKeyBasedFilterForDevices
                        .createExtraFilters(principal.getKey().getPermissions())) {
                    List<Predicate> filter = new ArrayList<>();
                    if (extraFilter.getDeviceGuids() != null) {
                        filter.add(from.join("device").get("guid").in(extraFilter.getDeviceGuids()));
                    }
                    if (extraFilter.getNetworkIds() != null) {
                        filter.add(from.join("device").join("network").get("id").in(extraFilter.getNetworkIds()));
                    }
                    extraPredicates.add(criteriaBuilder.and(filter.toArray(new Predicate[0])));
                }
                predicates.add(criteriaBuilder.or(extraPredicates.toArray(new Predicate[0])));
            }
        }
    }
}
