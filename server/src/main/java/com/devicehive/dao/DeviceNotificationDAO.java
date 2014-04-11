package com.devicehive.dao;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Constants;
import com.devicehive.dao.filter.AccessKeyBasedFilterForDevices;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.User;
import com.devicehive.util.LogExecutionTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.*;

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

    @SuppressWarnings("unchecked")
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    //TODO fix me. Incorrect query
    public List<DeviceNotification> queryDeviceNotification(Device device,
                                                            Timestamp start,
                                                            Timestamp end,
                                                            String notification,
                                                            String sortField,
                                                            Boolean sortOrderAsc,
                                                            Integer take,
                                                            Integer skip,
                                                            Integer gridInterval) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<DeviceNotification> criteria = criteriaBuilder.createQuery(DeviceNotification.class);
        Root<DeviceNotification> from = criteria.from(DeviceNotification.class);
        List<Predicate> predicates = new ArrayList<>();
        //where
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

        //groupBy
        if (gridInterval != null) {
            Subquery<Timestamp> timestampSubquery = gridIntervalFilter(criteriaBuilder, criteria,
                    gridInterval, from.<Timestamp>get("timestamp"));
            predicates.add(from.get("timestamp").in(timestampSubquery));
        }

        criteria.where(predicates.toArray(new Predicate[predicates.size()]));
        //orderBy
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
        try {
            List<DeviceNotification> result = resultQuery.getResultList();
            return result;
        } catch (Exception e) {
            return Collections.EMPTY_LIST;
        }
    }

    private Subquery<Timestamp> gridIntervalFilter(CriteriaBuilder cb,
                                                   CriteriaQuery<DeviceNotification> criteria,
                                                   Integer gridInterval,
                                                   Expression<Timestamp> exp) {
        Subquery<Timestamp> timestampSubquery = criteria.subquery(Timestamp.class);
        Root<DeviceNotification> subqueryFrom = timestampSubquery.from(DeviceNotification.class);
        timestampSubquery.select(cb.least(subqueryFrom.<Timestamp>get("timestamp")));
        List<Expression<?>> groupExpressions = new ArrayList<>();
        groupExpressions.add(cb.function("get_first_timestamp", Long.class, cb.literal(gridInterval), exp));
        groupExpressions.add(subqueryFrom.get("device"));
        groupExpressions.add(subqueryFrom.get("notification"));
        timestampSubquery.groupBy(groupExpressions);
        return timestampSubquery;
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
