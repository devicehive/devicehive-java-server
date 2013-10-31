package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
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
public class DeviceCommandDAO {

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    public DeviceCommand createCommand(DeviceCommand deviceCommand) {
        em.persist(deviceCommand);
        em.refresh(deviceCommand);
        return deviceCommand;
    }

    public boolean deleteById(@NotNull Long id) {
        Query query = em.createNamedQuery("DeviceCommand.deleteById");
        query.setParameter("id", id);
        return query.executeUpdate() != 0;
    }

    public int deleteByFK(@NotNull Device device) {
        Query query = em.createNamedQuery("DeviceCommand.deleteByFK");
        query.setParameter("device", device);
        return query.executeUpdate();
    }

    public int deleteCommand(@NotNull Device device, @NotNull User user) {
        Query query = em.createNamedQuery("DeviceCommand.deleteByDeviceAndUser");
        query.setParameter("user", user);
        query.setParameter("device", device);
        return query.executeUpdate();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceCommand findById(Long id) {
        return em.find(DeviceCommand.class, id);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceCommand getWithDevice(@NotNull long id) {

        TypedQuery<DeviceCommand> query = em.createNamedQuery("DeviceCommand.getWithDeviceById", DeviceCommand.class);
        query.setParameter("id", id);
        CacheHelper.cacheable(query);
        List<DeviceCommand> resultList = query.getResultList();

        return resultList.isEmpty() ? null : resultList.get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceCommand getByDeviceGuidAndId(@NotNull String guid, @NotNull long id) {

        TypedQuery<DeviceCommand> query =
                em.createNamedQuery("DeviceCommand.getByDeviceUuidAndId", DeviceCommand.class);
        query.setParameter("id", id);
        query.setParameter("guid", guid);
        CacheHelper.cacheable(query);
        List<DeviceCommand> resultList = query.getResultList();

        return resultList.isEmpty() ? null : resultList.get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceCommand getWithDeviceAndUser(@NotNull long id) {

        TypedQuery<DeviceCommand> query =
                em.createNamedQuery("DeviceCommand.getWithDeviceAndUserById", DeviceCommand.class);
        query.setParameter("id", id);

        List<DeviceCommand> resultList = query.getResultList();

        return resultList.isEmpty() ? null : resultList.get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceCommand> getCommandsListForPolling(List<Device> devices, List<String> names, User user,
                                                         Timestamp timestamp) {

        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<DeviceCommand> criteria = criteriaBuilder.createQuery(DeviceCommand.class);
        Root<DeviceCommand> from = criteria.from(DeviceCommand.class);
        List<Predicate> predicates = new ArrayList<>();

        if (devices != null && !devices.isEmpty()) {
            predicates.add(from.get("device").in(devices));
        }

        predicates.add(criteriaBuilder.greaterThan(from.<Timestamp>get("timestamp"), timestamp));

        if (user != null && !user.isAdmin()) {
            Path<User> path = from.join("device").join("network").join("users");
            predicates.add(path.in(user));
        }

        if (names != null && !names.isEmpty()) {
            predicates.add(from.get("command").in(names));
        }
        criteria.where(predicates.toArray(new Predicate[predicates.size()]));
        return em.createQuery(criteria).getResultList();
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
                                                  Integer skip) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<DeviceCommand> criteria = criteriaBuilder.createQuery(DeviceCommand.class);
        Root from = criteria.from(DeviceCommand.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.equal(from.get("device"), device));
        if (start != null) {
            predicates.add(criteriaBuilder.greaterThan(from.get("timestamp"), start));
        }
        if (end != null) {
            predicates.add(criteriaBuilder.lessThan(from.get("timestamp"), end));
        }
        if (command != null) {
            predicates.add(criteriaBuilder.equal(from.get("command"), command));
        }
        if (status != null) {
            predicates.add(criteriaBuilder.equal(from.get("status"), status));
        }

        criteria.where(predicates.toArray(new Predicate[predicates.size()]));
        if (sortField != null) {
            if (sortOrderAsc == null || sortOrderAsc) {
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
            resultQuery.setMaxResults(take);
        }
        return resultQuery.getResultList();

    }
}
