package com.devicehive.dao;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Constants;
import com.devicehive.dao.filter.AccessKeyBasedFilterForDevices;
import com.devicehive.model.Device;
import com.devicehive.model.User;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

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
import java.util.Collection;
import java.util.List;

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
        final StringBuilder format = new StringBuilder("SELECT * FROM device d LEFT JOIN network n ON d.network_id = n.id LEFT JOIN device_class dc " +
                "ON d.device_class_id = dc.id LEFT JOIN equipment e ON e.device_class_id = d.device_class_id LEFT JOIN user_network un ON un.network_id = n.id ");
        List<String> conditions = new ArrayList<>();

        //device fields filters
        if (namePattern != null) {
            conditions.add(String.format("d.name LIKE '%s'", namePattern));
        } else {
            if (name != null) {
                conditions.add(String.format("d.name = '%s'", name));
            }
        }
        if (status != null) {
            conditions.add(String.format("d.status = '%s'", status));
        }

        if (networkId != null) {
            conditions.add(String.format("d.network_id = %s", networkId));
        }

        if (networkName != null) {
            conditions.add(String.format("n.name = '%s'", networkName));
        }

        if (deviceClassId != null) {
            conditions.add(String.format("d.device_class_id = %s", deviceClassId));
        }

        if (deviceClassName != null) {
            conditions.add(String.format("dc.name = '%s'", deviceClassName));
        }

        if (deviceClassVersion != null) {
            conditions.add(String.format("dc.version = '%s'", deviceClassVersion));
        }

        if (principal != null) {
            User user = principal.getUser();
            if (user == null && principal.getKey() != null) {
                user = principal.getKey().getUser();
            }
            if (user != null && !user.isAdmin()) {
                conditions.add(String.format("un.user_id = %s", user.getId()));
            }
            if (principal.getDevice() != null) {
                conditions.add(String.format("d.id = %s", principal.getDevice().getId()));
            }
            if (principal.getKey() != null) {

                for (AccessKeyBasedFilterForDevices extraFilter : AccessKeyBasedFilterForDevices
                        .createExtraFilters(principal.getKey().getPermissions())) {
                    if (extraFilter.getDeviceGuids() != null) {
                        conditions.add(String.format("d.guid IN (%s)", String.format("'%s'",
                                StringUtils.join(extraFilter.getDeviceGuids(), "','"))));
                    }
                    if (extraFilter.getNetworkIds() != null) {
                        conditions.add(String.format("n.id IN (%s)", StringUtils.join(extraFilter.getNetworkIds(), ",")));
                    }
                }
            }
        }

        if (!conditions.isEmpty()) {
            format.append(" WHERE ").append(StringUtils.join(conditions, " AND "));
        }

        if (sortField != null) {
            format.append(String.format(" ORDER BY %s ", sortField));
            if (sortOrderAsc != null && !sortOrderAsc) {
                format.append(" DESC ");
            }
        }
        if (skip != null) {
            format.append(String.format(" OFFSET %s ", skip));
        }
        if (take == null) {
            take = Constants.DEFAULT_TAKE;
        }
        format.append(String.format(" LIMIT %s", take));
        Query  resultQuery = em.createNativeQuery(format.toString(), Device.class);
        CacheHelper.cacheable(resultQuery);
        return (List<Device>) resultQuery.getResultList();
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
