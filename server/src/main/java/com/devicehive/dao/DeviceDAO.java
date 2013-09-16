package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.*;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Stateless
@EJB(beanInterface = DeviceDAO.class, name = "DeviceDAO")
public class DeviceDAO {

    @EJB
    private NetworkDAO networkDAO;
    @EJB
    private DeviceClassDAO deviceClassDAO;
    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Device findById(Long id) {
        return em.find(Device.class, id);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Device findByUUID(String uuid) {
        TypedQuery<Device> query = em.createNamedQuery("Device.findByUUID", Device.class);
        query.setParameter("uuid", uuid);
        CacheHelper.cacheable(query);
        List<Device> res = query.getResultList();
        return res.isEmpty() ? null : res.get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Device findByUUIDWithNetworkAndDeviceClass(String uuid) {
        TypedQuery<Device> query = em.createNamedQuery("Device.findByUUIDWithNetworkAndDeviceClass", Device.class);
        query.setParameter("uuid", uuid);
        List<Device> res = query.getResultList();
        return res.isEmpty() ? null : res.get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Device findByUUIDAndKey(String uuid, String key) {
        TypedQuery<Device> query = em.createNamedQuery("Device.findByUUIDAndKey", Device.class);
        query.setParameter("uuid", uuid);
        query.setParameter("key", key);
        CacheHelper.cacheable(query);
        return query.getResultList().isEmpty() ? null : query.getResultList().get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Device findByUUID(String uuid, Long userId) {
        TypedQuery<Device> query = em.createNamedQuery("Device.findByUUIDForUser", Device.class);
        query.setParameter("uuid", uuid);
        query.setParameter("userId", userId);
        return query.getResultList().isEmpty() ? null : query.getResultList().get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Device> findByUUIDListAndUser(User user, List<String> list) {
        TypedQuery<Device> query = em.createNamedQuery("Device.findByUUIDListAndUser", Device.class);
        query.setParameter("user", user);
        query.setParameter("guidList", list);
        return query.getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Device findByUUIDAndUser(User user, String guid) {
        TypedQuery<Device> query = em.createNamedQuery("Device.findByUUIDAndUser", Device.class);
        query.setParameter("user", user);
        query.setParameter("guid", guid);
        return query.getResultList().isEmpty() ? null : query.getResultList().get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Device> findByUUID(List<String> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        TypedQuery<Device> query = em.createNamedQuery("Device.findByListUUID", Device.class);
        query.setParameter("guidList", list);
        return query.getResultList();
    }

    public Device createDevice(Device device) {
        em.persist(device);
        return device;
    }

    public Device setOffline(long id) {
        Device device = findById(id);
        device.setStatus("Offline");
        return device;
    }

    public boolean deleteDevice(@NotNull Long id) {
        Query query = em.createNamedQuery("Device.deleteById");
        query.setParameter("id", id);
        return query.executeUpdate() != 0;
    }

    public boolean deleteDevice(@NotNull String guid) {
        Query query = em.createNamedQuery("Device.deleteByUUID");
        query.setParameter("guid", guid);
        return query.executeUpdate() != 0;
    }

    public int deleteDeviceByFK(@NotNull Network network) {
        Query query = em.createNamedQuery("Device.deleteByNetwork");
        query.setParameter("network", network);
        return query.executeUpdate();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Device> getList(String name,
                                String namePattern,
                                String status,
                                Long networkId,
                                String networkName,
                                Long deviceClassId,
                                String deviceClassName,
                                String deviceClassVersion,
                                String sortField,
                                Boolean sortOrderAsc,
                                Integer take,
                                Integer skip,
                                User user,
                                Set<Long> allowedNetworkIds,
                                Set<String> allowedDeviceGuids) {

        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Device> deviceCriteria = criteriaBuilder.createQuery(Device.class);
        Root fromDevice = deviceCriteria.from(Device.class);
        fromDevice.fetch("network", JoinType.LEFT);
        fromDevice.fetch("deviceClass");
        List<Predicate> devicePredicates = new ArrayList<>();

        //device fields filters
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
        if (allowedDeviceGuids !=null && !allowedDeviceGuids.contains(null)){
            devicePredicates.add((fromDevice.get("status").in(allowedDeviceGuids)));
        }

        //network subcriteria building
        if (allowedNetworkIds == null && (networkId != null || networkName != null)) {
            List<Network> networksResult = networkDAO.getByNameOrId(networkId, networkName);

            if (networksResult.size() == 0) {
                return new ArrayList<>();
            }

            Expression<Network> inExpression = fromDevice.get("network");
            devicePredicates.add(inExpression.in(networksResult));
        } else if (allowedNetworkIds != null && !allowedNetworkIds.contains(null)) {
            List<Network> networkResult;
            if (networkId != null || networkName != null) {
                networkResult = networkDAO.getByNameOrId(networkId, networkName);

                if (networkResult.size() == 0) {
                    return new ArrayList<>();
                }
                if (!allowedNetworkIds.contains(networkResult.get(0).getId())) {
                    return new ArrayList<>();            //found network is not in allowed list
                }
            } else {
                networkResult = new ArrayList<>();
                for (Long allowedNetworkId : allowedNetworkIds) {
                    networkResult.add(networkDAO.getById(allowedNetworkId));
                }
                if (networkResult.isEmpty()) {
                    return new ArrayList<>();
                }
            }
            Expression<Network> inExpression = fromDevice.get("network");
            devicePredicates.add(inExpression.in(networkResult));
        } else {
            if (user.getRole().equals(UserRole.CLIENT)) {
                Path<User> path = fromDevice.join("network").join("users");
                devicePredicates.add(path.in(user));
            }
        }

        //deviceclass subcriteria building
        if (deviceClassId != null || deviceClassName != null || deviceClassVersion != null) {
            List<DeviceClass> deviceClassResult = deviceClassDAO.getByIdOrNameOrVersion(deviceClassId,
                    deviceClassName, deviceClassVersion);
            if (deviceClassResult.size() == 0) {
                return new ArrayList<>();
            }
            Expression<DeviceClass> inExpression = fromDevice.get("deviceClass");
            devicePredicates.add(inExpression.in(deviceClassResult));
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
            take = Constants.DEFAULT_TAKE;
        }
        resultQuery.setMaxResults(take);
        return resultQuery.getResultList();
    }
}
