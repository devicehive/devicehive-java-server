package com.devicehive.dao;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Constants;
import com.devicehive.dao.filter.AccessKeyBasedFilterForDevices;
import com.devicehive.model.Device;
import com.devicehive.model.Network;
import com.devicehive.model.User;

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
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.devicehive.model.Device.Queries.Names.DELETE_BY_ID;
import static com.devicehive.model.Device.Queries.Names.DELETE_BY_NETWORK;
import static com.devicehive.model.Device.Queries.Names.DELETE_BY_UUID;
import static com.devicehive.model.Device.Queries.Names.FIND_BY_NETWORK;
import static com.devicehive.model.Device.Queries.Names.FIND_BY_UUID_AND_KEY;
import static com.devicehive.model.Device.Queries.Names.FIND_BY_UUID_LIST_AND_NETWORK;
import static com.devicehive.model.Device.Queries.Names.FIND_BY_UUID_WITH_NETWORK_AND_DEVICE_CLASS;
import static com.devicehive.model.Device.Queries.Parameters.GUID;
import static com.devicehive.model.Device.Queries.Parameters.GUID_LIST;
import static com.devicehive.model.Device.Queries.Parameters.ID;
import static com.devicehive.model.Device.Queries.Parameters.KEY;
import static com.devicehive.model.Device.Queries.Parameters.NETWORK;

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
    public Device findByUUIDWithNetworkAndDeviceClass(String uuid) {
        TypedQuery<Device> query = em.createNamedQuery(FIND_BY_UUID_WITH_NETWORK_AND_DEVICE_CLASS,
                Device.class);
        query.setParameter(GUID, uuid);
        List<Device> res = query.getResultList();
        return res.isEmpty() ? null : res.get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Device findByUUIDAndKey(String uuid, String key) {
        TypedQuery<Device> query = em.createNamedQuery(FIND_BY_UUID_AND_KEY, Device.class);
        query.setParameter(GUID, uuid);
        query.setParameter(KEY, key);
        CacheHelper.cacheable(query);
        return query.getResultList().isEmpty() ? null : query.getResultList().get(0);
    }

    public List<Device> findByUUIDListAndNetwork(Collection<String> list, Network network) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        TypedQuery<Device> query = em.createNamedQuery(FIND_BY_UUID_LIST_AND_NETWORK, Device.class);
        query.setParameter(NETWORK, network);
        query.setParameter(GUID_LIST, list);
        return query.getResultList();
    }

    public List<Device> findByNetwork(Network network) {
        TypedQuery<Device> query = em.createNamedQuery(FIND_BY_NETWORK, Device.class);
        query.setParameter(NETWORK, network);
        return query.getResultList();
    }

    public Device createDevice(Device device) {
        em.persist(device);
        return device;
    }

    public Device mergeDevice(Device device) {
        em.merge(device);
        return device;
    }

    public Device setOffline(long id) {
        Device device = findById(id);
        device.setStatus("Offline");
        return device;
    }

    public boolean deleteDevice(@NotNull Long id) {
        Query query = em.createNamedQuery(DELETE_BY_ID);
        query.setParameter(ID, id);
        return query.executeUpdate() != 0;
    }

    public boolean deleteDevice(@NotNull String guid) {
        Query query = em.createNamedQuery(DELETE_BY_UUID);
        query.setParameter(GUID, guid);
        return query.executeUpdate() != 0;
    }

    public int deleteDeviceByFK(@NotNull Network network) {
        Query query = em.createNamedQuery(DELETE_BY_NETWORK);
        query.setParameter(NETWORK, network);
        return query.executeUpdate();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public long getNumberOfAvailableDevices(HivePrincipal principal, List<String> guids) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = criteriaBuilder.createQuery(Long.class);
        Root<Device> from = criteria.from(Device.class);
        List<Predicate> predicates = new ArrayList<>();
        appendPrincipalPredicates(predicates, principal, from);
        if (guids != null && !guids.isEmpty()) {
            predicates.add(from.get(GUID).in(guids));
        }
        criteria.where(predicates.toArray(new Predicate[predicates.size()]));
        criteria.select(criteriaBuilder.count(from));
        TypedQuery<Long> query = em.createQuery(criteria);
        return query.getSingleResult();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Device> getDeviceList(HivePrincipal principal, Collection<String> guids) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Device> criteria = criteriaBuilder.createQuery(Device.class);
        Root<Device> from = criteria.from(Device.class);
        List<Predicate> predicates = new ArrayList<>();
        appendPrincipalPredicates(predicates, principal, from);
        if (guids != null && !guids.isEmpty()) {
            predicates.add(from.get(GUID).in(guids));
        }
        criteria.where(predicates.toArray(new Predicate[predicates.size()]));
        TypedQuery<Device> query = em.createQuery(criteria);
        return query.getResultList();
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
                                HivePrincipal principal) {

        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Device> deviceCriteria = criteriaBuilder.createQuery(Device.class);
        Root<Device> fromDevice = deviceCriteria.from(Device.class);
        fromDevice.fetch("network", JoinType.LEFT);
        fromDevice.fetch("deviceClass").fetch("equipment", JoinType.LEFT);
        List<Predicate> devicePredicates = new ArrayList<>();

        //device fields filters
        if (namePattern != null) {
            devicePredicates.add(criteriaBuilder.like(fromDevice.<String>get("name"), namePattern));
        } else {
            if (name != null) {
                devicePredicates.add(criteriaBuilder.equal(fromDevice.get("name"), name));
            }
        }
        if (status != null) {
            devicePredicates.add(criteriaBuilder.equal(fromDevice.get("status"), status));
        }

        if (networkId != null) {
            devicePredicates.add(criteriaBuilder.equal(fromDevice.get("network").get("id"), networkId));
        }

        if (networkName != null) {
            devicePredicates.add(criteriaBuilder.equal(fromDevice.get("network").get("name"), networkName));
        }

        if (deviceClassId != null) {
            devicePredicates.add(criteriaBuilder.equal(fromDevice.get("deviceClass").get("id"), deviceClassId));
        }

        if (deviceClassName != null) {
            devicePredicates.add(criteriaBuilder.equal(fromDevice.get("deviceClass").get("name"), deviceClassName));
        }

        if (deviceClassVersion != null) {
            devicePredicates
                    .add(criteriaBuilder.equal(fromDevice.get("deviceClass").get("version"), deviceClassVersion));
        }

        appendPrincipalPredicates(devicePredicates, principal, fromDevice);


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

    private void appendPrincipalPredicates(List<Predicate> devicePredicates, HivePrincipal principal,
                                           Root<Device> fromDevice) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        if (principal != null) {
            User user = principal.getUser();
            if (user == null && principal.getKey() != null) {
                user = principal.getKey().getUser();
            }
            if (user != null && !user.isAdmin()) {
                Path<User> path = fromDevice.join("network").join("users");
                devicePredicates.add(path.in(user));
            }
            if (principal.getDevice() != null) {
                devicePredicates.add(criteriaBuilder.equal(fromDevice.get("id"), principal.getDevice().getId()));
            }
            if (principal.getKey() != null) {

                List<Predicate> extraPredicates = new ArrayList<>();
                for (AccessKeyBasedFilterForDevices extraFilter : AccessKeyBasedFilterForDevices
                        .createExtraFilters(principal.getKey().getPermissions())) {
                    List<Predicate> filter = new ArrayList<>();
                    if (extraFilter.getDeviceGuids() != null) {
                        filter.add(fromDevice.get("guid").in(extraFilter.getDeviceGuids()));
                    }
                    if (extraFilter.getNetworkIds() != null) {
                        filter.add(fromDevice.get("network").get("id").in(extraFilter.getNetworkIds()));
                    }
                    extraPredicates.add(criteriaBuilder.and(filter.toArray(new Predicate[0])));
                }
                devicePredicates.add(criteriaBuilder.or(extraPredicates.toArray(new Predicate[0])));
            }
        }
    }
}
