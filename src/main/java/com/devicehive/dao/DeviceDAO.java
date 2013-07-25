package com.devicehive.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devicehive.configuration.Constants;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Network;
import com.devicehive.model.User;

@Stateless
@EJB(beanInterface = DeviceDAO.class, name = "DeviceDAO")
public class DeviceDAO {

    private static Logger logger = LoggerFactory.getLogger(DeviceDAO.class);
    
    private static final Integer DEFAULT_TAKE = 1000; //TODO set parameter
    
    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Device findById(Long id) {
        return em.find(Device.class, id);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Device findByUUID(UUID uuid) {
        TypedQuery<Device> query = em.createNamedQuery("Device.findByUUID", Device.class);
        query.setParameter("uuid", uuid);
        List<Device> res = query.getResultList();
        return res.isEmpty() ? null : res.get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Device findByUUIDAndKey(UUID uuid, String key) {
        TypedQuery<Device> query = em.createNamedQuery("Device.findByUUIDAndKey", Device.class);
        query.setParameter("uuid", uuid);
        query.setParameter("key", key);
        return query.getResultList().isEmpty() ? null : query.getResultList().get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Device> findByUUIDListAndUser(User user, List<UUID> list) {
        TypedQuery<Device> query = em.createNamedQuery("Device.findByUUIDListAndUser", Device.class);
        query.setParameter("user", user);
        query.setParameter("guidList", list);
        return query.getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Device findByUUIDAndUser(User user, UUID guid) {
        TypedQuery<Device> query = em.createNamedQuery("Device.findByUUIDAndUser", Device.class);
        query.setParameter("user", user);
        query.setParameter("guid", guid);
        return query.getResultList().isEmpty() ? null : query.getResultList().get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Device> findByUUID(List<UUID> list) {
        TypedQuery<Device> query = em.createNamedQuery("Device.findByListUUID", Device.class);
        query.setParameter("guidList", list);
        return query.getResultList();
    }

    public Device createDevice(Device device) {
        em.persist(device);
        return device;
    }

    public boolean updateDevice(@NotNull Long id, Device device) {
        Query query = em.createNamedQuery("Device.updateById");
        query.setParameter("name", device.getName());
        query.setParameter("status", device.getStatus());
        query.setParameter("network", device.getNetwork());
        query.setParameter("deviceClass", device.getClass());
        query.setParameter("data", device.getData());
        query.setParameter("id", device.getId());
        return query.executeUpdate() != 0;
    }

    public boolean deleteDevice(@NotNull Long id) {
        Query query = em.createNamedQuery("Device.deleteById");
        query.setParameter("id", id);
        return query.executeUpdate() != 0;
    }

    public int deleteDeviceByFK(@NotNull Network network) {
        Query query = em.createNamedQuery("Device.deleteByNetwork");
        query.setParameter("network", network);
        return query.executeUpdate();
    }

    //TODO refactor
    @SuppressWarnings("unchecked")
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Device> getList(String name, String namePattern, String status, Long networkId,
                                String networkName, Long deviceClassId, String deviceClassName,
                                String deviceClassVersion, String sortField,
                                Boolean sortOrderAsc, Integer take, Integer skip) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Device> deviceCriteria = criteriaBuilder.createQuery(Device.class);
        Root fromDevice = deviceCriteria.from(Device.class);
        List<Predicate> devicePredicates = new ArrayList<>();
        if (namePattern != null) {
            devicePredicates.add(criteriaBuilder.like(fromDevice.get("name"), namePattern));
        } else {
            if (name != null) {
                devicePredicates.add(criteriaBuilder.equal(fromDevice.get("name"), name));
            }
        }
        if (status != null) {
            devicePredicates.add(criteriaBuilder.equal(fromDevice.get("status"), status));
        }
        // to network dao
        if (networkId != null || networkName != null) {
            CriteriaQuery<Network> networkCriteria = criteriaBuilder.createQuery(Network.class);
            Root fromNetwork = networkCriteria.from(Network.class);
            List<Predicate> networkPredicates = new ArrayList<>();
            if (networkId != null) {
                networkPredicates.add(criteriaBuilder.equal(fromNetwork.get("id"), networkId));
            }
            if (networkName != null) {
                networkPredicates.add(criteriaBuilder.equal(fromNetwork.get("name"), networkName));
            }
            networkCriteria.where(networkPredicates.toArray(new Predicate[networkPredicates.size()]));
            TypedQuery<Network> networksQuery = em.createQuery(networkCriteria);
            List<Network> networksResult = networksQuery.getResultList();
            if (networksResult.size() == 0) {
                return new ArrayList<>();
            }
            Expression<Network> inExpression = fromDevice.get("network");
            devicePredicates.add(inExpression.in(networksResult));
        }
        //to deviceClassDAO
        if (deviceClassId != null || deviceClassName != null || deviceClassVersion != null) {
            CriteriaQuery<DeviceClass> deviceClassCriteria = criteriaBuilder.createQuery(DeviceClass.class);
            Root fromDeviceClass = deviceClassCriteria.from(DeviceClass.class);
            List<Predicate> deviceClassPredicates = new ArrayList<>();
            if (deviceClassId != null) {
                deviceClassPredicates.add(criteriaBuilder.equal(fromDeviceClass.get("id"), deviceClassId));
            }
            if (deviceClassName != null) {
                deviceClassPredicates.add(criteriaBuilder.equal(fromDeviceClass.get("name"), name));
            }
            if (deviceClassVersion != null) {
                deviceClassPredicates.add(criteriaBuilder.equal(fromDeviceClass.get("version"), deviceClassVersion));
            }
            deviceClassCriteria.where(deviceClassPredicates.toArray(new Predicate[deviceClassPredicates.size()]));
            TypedQuery<DeviceClass> deviceClassQuery = em.createQuery(deviceClassCriteria);
            List<DeviceClass> deviceClassResult = deviceClassQuery.getResultList();
            if (deviceClassResult.size() == 0) {
                return new ArrayList<>();
            }
            Expression<DeviceClass> inExpresion = fromDevice.get("deviceClass");
            devicePredicates.add(inExpresion.in(deviceClassResult));
        }
        deviceCriteria.where(devicePredicates.toArray(new Predicate[devicePredicates.size()]));
        if (sortField != null) {
            if (sortOrderAsc == null || sortOrderAsc) {
                deviceCriteria.orderBy(criteriaBuilder.asc(fromDevice.get(sortField)));
            } else {
                deviceCriteria.orderBy(criteriaBuilder.desc(fromDevice.get(sortField)));
            }
        }

        TypedQuery<Device> resultQuery = em.createQuery(deviceCriteria);
        if (skip != null) {
            resultQuery.setFirstResult(skip);
        }
        if (take == null) {
            take = DEFAULT_TAKE;
        }
        resultQuery.setMaxResults(take);
        return resultQuery.getResultList();
    }


}
