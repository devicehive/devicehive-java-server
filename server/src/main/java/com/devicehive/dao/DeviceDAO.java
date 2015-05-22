package com.devicehive.dao;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Constants;
import com.devicehive.dao.filter.AccessKeyBasedFilterForDevices;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.*;
import javax.persistence.criteria.*;
import javax.validation.constraints.NotNull;
import java.util.*;

import static com.devicehive.model.Device.Queries.Names.*;
import static com.devicehive.model.Device.Queries.Parameters.GUID;
import static com.devicehive.model.Device.Queries.Parameters.KEY;

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
        CacheHelper.cacheable(query);
        query.setHint(CacheHelper.STORE_MODE, CacheStoreMode.REFRESH);
        List<Device> res = query.getResultList();
        return res.isEmpty() ? null : res.get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Device findByUUIDAndKey(String uuid, String key) {
        TypedQuery<Device> query = em.createNamedQuery(FIND_BY_UUID_AND_KEY, Device.class);
        query.setParameter(GUID, uuid);
        query.setParameter(KEY, key);
        CacheHelper.cacheable(query);
        List<Device> devices = query.getResultList();
        return devices.isEmpty() ? null : devices.get(0);
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

    public Device setOffline(String guid) {
        Device device = findByUUIDWithNetworkAndDeviceClass(guid);
        device.setStatus("Offline");
        return device;
    }

    public boolean deleteDevice(@NotNull String guid) {
        Query query = em.createNamedQuery(DELETE_BY_UUID);
        query.setParameter(GUID, guid);
        return query.executeUpdate() != 0;
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
        if (!CollectionUtils.isEmpty(guids)) {
            predicates.add(from.get(GUID).in(guids));
        }
        criteria.where(predicates.toArray(new Predicate[predicates.size()]));
        TypedQuery<Device> query = em.createQuery(criteria);
        CacheHelper.cacheable(query);
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
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
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Device> deviceQuery = cb.createQuery(Device.class);
        Root<Device> root = deviceQuery.from(Device.class);
        Join<Device, Network> n = (Join) root.fetch("network", JoinType.LEFT);
        Join<Device, Network> un = (Join) n.fetch("users", JoinType.LEFT);
        Join<Device, DeviceClass> dc = (Join) root.fetch("deviceClass", JoinType.LEFT);
        Join<Object, Object> eq = (Join) dc.fetch("equipment", JoinType.LEFT);

        List<Predicate> predicates = new LinkedList<>();

        if (namePattern != null) {
            predicates.add(cb.like(root.<String>get("name"), namePattern));
        } else if (name != null) {
            predicates.add(cb.equal(root.<String>get("name"), name));
        }
        if (status != null) {
            predicates.add(cb.equal(root.<String>get("status"), status));
        }
        if (networkId != null) {
            predicates.add(cb.equal(n.<Long>get("id"), networkId));
        }
        if (networkName != null) {
            predicates.add(cb.equal(n.<String>get("name"), networkName));
        }
        if (deviceClassId != null) {
            predicates.add(cb.equal(dc.<Long>get("id"), deviceClassId));
        }
        if (deviceClassName != null) {
            predicates.add(cb.equal(dc.<String>get("name"), deviceClassName));
        }
        if (deviceClassVersion != null) {
            predicates.add(cb.equal(dc.<String>get("version"), deviceClassVersion));
        }
        if (principal != null) {
            User user = principal.getUser();
            if (user == null && principal.getKey() != null) {
                user = principal.getKey().getUser();
            }
            if (user != null && !user.isAdmin()) {
                predicates.add(cb.equal(un.<Long>get("id"), user.getId()));
            }
            if (principal.getDevice() != null) {
                predicates.add(cb.equal(root.<Long>get("id"), principal.getDevice().getId()));
            }
            if (principal.getKey() != null) {
                for (AccessKeyBasedFilterForDevices extraFilter : AccessKeyBasedFilterForDevices.createExtraFilters(principal.getKey().getPermissions())) {
                    if (extraFilter.getDeviceGuids() != null) {
                        predicates.add(root.<String>get("guid").in(extraFilter.getDeviceGuids()));
                    }
                    if (extraFilter.getNetworkIds() != null) {
                        predicates.add(n.<Long>get("id").in(extraFilter.getNetworkIds()));
                    }
                }
            }
        }
        deviceQuery.select(root);
        if (CollectionUtils.isNotEmpty(predicates)) {
            deviceQuery.where(predicates.toArray(new Predicate[predicates.size()]));
        }

        if (sortField != null) {
            if (Boolean.TRUE.equals(sortOrderAsc)) {
                deviceQuery.orderBy(cb.asc(root.get(sortField)));
            } else {
                deviceQuery.orderBy(cb.desc(root.get(sortField)));
            }
        }
        TypedQuery<Device> query = em.createQuery(deviceQuery);
        if (skip != null) {
            query.setFirstResult(skip);
        }
        query.setMaxResults(take == null ? Constants.DEFAULT_TAKE : take);
        query.setHint(CacheHelper.CACHEBLE, true);
        query.setHint(CacheHelper.STORE_MODE, CacheStoreMode.REFRESH);
        return query.getResultList();
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
                    extraPredicates.add(criteriaBuilder.and(filter.toArray(new Predicate[filter.size()])));
                }
                devicePredicates
                    .add(criteriaBuilder.or(extraPredicates.toArray(new Predicate[extraPredicates.size()])));
            }
        }
    }
}
