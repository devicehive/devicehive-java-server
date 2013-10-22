package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.dao.filter.AccessKeyBasedFilterForDevices;
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
import java.util.*;

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

    public List<Device> findByUUIDListAndNetwork(Collection<String> list, Network network) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        TypedQuery<Device> query = em.createNamedQuery("Device.findByUUIDListAndNetwork", Device.class);
        query.setParameter("network", network);
        query.setParameter("guidList", list);
        return query.getResultList();
    }

    public List<Device> findByNetwork(Network network) {
        TypedQuery<Device> query = em.createNamedQuery("Device.findByNetwork", Device.class);
        query.setParameter("network", network);
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

    public long getNumberOfAvailableDevices(User user, Set<AccessKeyPermission> permissions, List<String> guids){
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = criteriaBuilder.createQuery(Long.class);
        Root<Device> from = criteria.from(Device.class);
        criteria = buildCriteriaQuery(criteria, criteriaBuilder, user, permissions, guids);
        criteria.select(criteriaBuilder.count(from));
        TypedQuery<Long> query = em.createQuery(criteria);
        return query.getSingleResult();
    }

    public List<Device> getDeviceList(User user, Set<AccessKeyPermission> permissions, List<String> guids){
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Device> criteria = criteriaBuilder.createQuery(Device.class);
        criteria = buildCriteriaQuery(criteria, criteriaBuilder, user, permissions, guids);
        TypedQuery<Device> query = em.createQuery(criteria);
        return query.getResultList();
    }

    private <T> CriteriaQuery<T> buildCriteriaQuery(CriteriaQuery<T> criteria,
                                                    CriteriaBuilder criteriaBuilder,
                                                    User user,
                                                    Set<AccessKeyPermission> permissions,
                                                    List<String> guids){
        Root<Device> from = criteria.from(Device.class);
        List<Predicate> predicates = new ArrayList<>();

        if (user != null && !user.isAdmin()){
            Path<User> path = from.join("network", JoinType.LEFT).join("users");
            predicates.add(path.in(user));
        }

        if (permissions != null){
            Collection<AccessKeyBasedFilterForDevices> extraFilters =  AccessKeyBasedFilterForDevices
                    .createExtraFilters(permissions);

            if (extraFilters != null) {
                List<Predicate> extraPredicates = new ArrayList<>();
                for (AccessKeyBasedFilterForDevices extraFilter : extraFilters) {
                    List<Predicate> filter = new ArrayList<>();
                    if (extraFilter.getDeviceGuids() != null) {
                        filter.add(from.get("guid").in(extraFilter.getDeviceGuids()));
                    }
                    if (extraFilter.getNetworkIds() != null) {
                        filter.add(from.get("network").get("id").in(extraFilter.getNetworkIds()));
                    }
                    extraPredicates.add(criteriaBuilder.and(filter.toArray(new Predicate[0])));
                }
                predicates.add(criteriaBuilder.or(extraPredicates.toArray(new Predicate[0])));
            }
        }

        if (guids != null && !guids.isEmpty()){
            predicates.add(from.get("guid").in(guids));
        }

        criteria.where(predicates.toArray(new Predicate[predicates.size()]));
        return criteria;
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
                                Collection<AccessKeyBasedFilterForDevices> extraFilters) {

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

        if (user != null && !user.isAdmin()) {
            Path<User> path = fromDevice.join("network").join("users");
            devicePredicates.add(path.in(user));
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
            devicePredicates.add(criteriaBuilder.equal(fromDevice.get("deviceClass").get("version"), deviceClassVersion));
        }


        if (extraFilters != null) {
            List<Predicate> extraPredicates = new ArrayList<>();
            for (AccessKeyBasedFilterForDevices extraFilter : extraFilters) {
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
