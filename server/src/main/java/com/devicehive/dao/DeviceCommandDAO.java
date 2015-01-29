package com.devicehive.dao;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Constants;
import com.devicehive.dao.filter.AccessKeyBasedFilterForDevices;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.Network;
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
import java.util.Collection;
import java.util.List;

import static com.devicehive.model.DeviceCommand.Queries.Names.*;
import static com.devicehive.model.DeviceCommand.Queries.Parameters.*;

@Stateless
@LogExecutionTime
public class DeviceCommandDAO {

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    public DeviceCommand createCommand(DeviceCommand deviceCommand) {
        em.persist(deviceCommand);
        return deviceCommand;
    }

    public boolean deleteById(@NotNull Long id) {
        Query query = em.createNamedQuery(DELETE_BY_ID);
        query.setParameter(ID, id);
        return query.executeUpdate() != 0;
    }

    public int deleteByFK(@NotNull Device device) {
        Query query = em.createNamedQuery(DELETE_BY_FOREIGN_KEY);
        query.setParameter(DEVICE, device);
        return query.executeUpdate();
    }

    public int deleteCommand(@NotNull Device device, @NotNull User user) {
        Query query = em.createNamedQuery(DELETE_BY_DEVICE_AND_USER);
        query.setParameter(USER, user);
        query.setParameter(DEVICE, device);
        return query.executeUpdate();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceCommand findById(Long id) {
        return em.find(DeviceCommand.class, id);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceCommand getByDeviceGuidAndId(@NotNull String guid, @NotNull long id) {
        TypedQuery<DeviceCommand> query =
            em.createNamedQuery(GET_BY_DEVICE_UUID_AND_ID, DeviceCommand.class);
        query.setParameter(ID, id);
        query.setParameter(GUID, guid);
        CacheHelper.cacheable(query);
        List<DeviceCommand> resultList = query.getResultList();
        return resultList.isEmpty() ? null : resultList.get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceCommand> findCommands(Collection<Device> devices, Collection<String> names,
                                            @NotNull Timestamp timestamp, HivePrincipal principal) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<DeviceCommand> criteria = criteriaBuilder.createQuery(DeviceCommand.class);
        Root<DeviceCommand> from = criteria.from(DeviceCommand.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.greaterThan(from.<Timestamp>get(DeviceCommand.TIMESTAMP_COLUMN), timestamp));
        if (names != null) {
            predicates.add(from.get(DeviceCommand.COMMAND_COLUMN).in(names));
        }
        if (devices != null) {
            predicates.add(from.get(DeviceCommand.DEVICE_COLUMN).in(devices));
        }
        appendPrincipalPredicates(predicates, principal, from);
        criteria.where(predicates.toArray(new Predicate[predicates.size()]));
        TypedQuery<DeviceCommand> query = em.createQuery(criteria);
        CacheHelper.cacheable(query);
        return query.getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceCommand> queryDeviceCommand(Device device,
                                                  Timestamp start,
                                                  Timestamp end,
                                                  String command,
                                                  String status,
                                                  String sortField,
                                                  Boolean sortOrderAsc,
                                                  Integer take,
                                                  Integer skip,
                                                  Integer gridInterval) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<DeviceCommand> criteria = criteriaBuilder.createQuery(DeviceCommand.class);
        Root<DeviceCommand> from = criteria.from(DeviceCommand.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.equal(from.get(DeviceCommand.DEVICE_COLUMN), device));
        //where
        if (start != null) {
            predicates.add(criteriaBuilder.greaterThan(from.<Timestamp>get(DeviceCommand.TIMESTAMP_COLUMN), start));
        }
        if (end != null) {
            predicates.add(criteriaBuilder.lessThan(from.<Timestamp>get(DeviceCommand.TIMESTAMP_COLUMN), end));
        }
        if (command != null) {
            predicates.add(criteriaBuilder.equal(from.get(DeviceCommand.COMMAND_COLUMN), command));
        }
        if (status != null) {
            predicates.add(criteriaBuilder.equal(from.get(DeviceCommand.STATUS_COLUMN), status));
        }

        //groupBy
        if (gridInterval != null) {
            Subquery<Timestamp> timestampSubquery = gridIntervalFilter(criteriaBuilder, criteria,
                                                                       gridInterval, from.<Timestamp>get(
                DeviceCommand.TIMESTAMP_COLUMN));
            predicates.add(from.get(DeviceCommand.TIMESTAMP_COLUMN).in(timestampSubquery));
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

        TypedQuery<DeviceCommand> resultQuery = em.createQuery(criteria);
        if (skip != null) {
            resultQuery.setFirstResult(skip);
        }
        if (take == null) {
            take = Constants.DEFAULT_TAKE;
        }
        resultQuery.setMaxResults(take);
        return resultQuery.getResultList();

    }

    private Subquery<Timestamp> gridIntervalFilter(CriteriaBuilder cb,
                                                   CriteriaQuery<DeviceCommand> criteria,
                                                   Integer gridInterval,
                                                   Expression<Timestamp> exp) {
        Subquery<Timestamp> timestampSubquery = criteria.subquery(Timestamp.class);
        Root<DeviceCommand> subqueryFrom = timestampSubquery.from(DeviceCommand.class);
        timestampSubquery.select(cb.least(subqueryFrom.<Timestamp>get(DeviceCommand.TIMESTAMP_COLUMN)));
        List<Expression<?>> groupExpressions = new ArrayList<>();
        groupExpressions.add(cb.function("get_first_timestamp", Long.class, cb.literal(gridInterval), exp));
        groupExpressions.add(subqueryFrom.get(DeviceCommand.DEVICE_COLUMN));
        groupExpressions.add(subqueryFrom.get(DeviceCommand.COMMAND_COLUMN));
        timestampSubquery.groupBy(groupExpressions);
        return timestampSubquery;
    }

    private void appendPrincipalPredicates(List<Predicate> predicates, HivePrincipal principal,
                                           Root<DeviceCommand> from) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        if (principal != null) {
            User user = principal.getUser();
            if (user == null && principal.getKey() != null) {
                user = principal.getKey().getUser();
            }
            if (user != null && !user.isAdmin()) {
                Predicate userPredicate = from.join(DeviceCommand.DEVICE_COLUMN)
                    .join(Device.NETWORK_COLUMN)
                    .join(Network.USERS_ASSOCIATION).in(user);
                predicates.add(userPredicate);
            }
            if (principal.getDevice() != null) {
                Predicate devicePredicate = from.get(DeviceCommand.DEVICE_COLUMN).in(principal.getDevice());
                predicates.add(devicePredicate);
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
                    extraPredicates.add(criteriaBuilder.and(filter.toArray(new Predicate[filter.size()])));
                }
                predicates.add(criteriaBuilder.or(extraPredicates.toArray(new Predicate[extraPredicates.size()])));
            }
        }
    }
}
