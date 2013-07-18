package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Stateless
public class DeviceCommandDAO {

    private static final Integer DEFAULT_TAKE = Integer.valueOf(1000); //TODO set parameter
    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    public void saveCommand(DeviceCommand deviceCommand) {
        em.persist(deviceCommand);
    }

    public DeviceCommand updateCommand(DeviceCommand update, Device expectedDevice) {
        DeviceCommand cmd = em.find(DeviceCommand.class, update.getId());
        if (!cmd.getDevice().getId().equals(expectedDevice.getId())) {
            throw new HiveException("Device tries to update incorrect command");
        }
        cmd.setCommand(update.getCommand());
        cmd.setParameters(update.getParameters());
        cmd.setLifetime(update.getLifetime());
        cmd.setFlags(update.getFlags());
        cmd.setStatus(update.getStatus());
        cmd.setResult(update.getResult());
        return em.merge(cmd);

    }

    public DeviceCommand findById(Long id) {
        return em.find(DeviceCommand.class, id);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceCommand> getNewerThan(Device device, Date timestamp) {
        TypedQuery<DeviceCommand> query = em.createNamedQuery("DeviceCommand.getNewerThan", DeviceCommand.class);
        query.setParameter("timestamp", timestamp);
        query.setParameter("device", device);
        return query.getResultList();
    }

    public List<DeviceCommand> queryDeviceCommand(Device device, Date start, Date end, String command,
                                                  String status, String sortField, Boolean sortOrderAsc,
                                                  Integer take, Integer skip) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<DeviceCommand> criteria = criteriaBuilder.createQuery(DeviceCommand.class);
        Root from = criteria.from(DeviceCommand.class);
        List<Predicate> predicates = new ArrayList<>();

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
            take = DEFAULT_TAKE;
            resultQuery.setMaxResults(take);
        }
        return resultQuery.getResultList();

    }
}
