package com.devicehive.dao.rdbms;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.dao.CacheConfig;
import com.devicehive.dao.CacheHelper;
import com.devicehive.dao.CriteriaHelper;
import com.devicehive.model.Device;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.sql.DataSource;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

@Repository
public class DeviceDaoImpl extends GenericDaoImpl implements DeviceDao {
    private static final String GET_DEVICES_GUIDS_AND_OFFLINE_TIMEOUT = "SELECT d.guid, dc.offline_timeout FROM device d " +
                                                                        "LEFT JOIN device_class dc " +
                                                                        "ON dc.id = d.device_class_id " +
                                                                        "WHERE d.guid IN (:guids)";
    private static final String UPDATE_DEVICES_STATUSES = "UPDATE device SET status =:status WHERE guid IN (:guids)";

    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    /**
     * Method change statuses for devices with guids that consists in the list
     *
     * @param status new status
     * @param guids  list of guids
     */
    public void changeStatusForDevices(String status, List<String> guids) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("status", status);
        parameters.addValue("guids", guids);
        jdbcTemplate.update(UPDATE_DEVICES_STATUSES, parameters);
    }

    /**
     * Method return a Map where KEY is a device guid from guids list and
     * VALUE is OfflineTimeout from deviceClass for device with current guid.
     *
     * @param guids list of guids
     */
    public Map<String, Integer> getOfflineTimeForDevices(List<String> guids) {
        final Map<String, Integer> deviceInfo = new HashMap<>();
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("guids", guids);
        List<Map<String, Object>> results = jdbcTemplate.queryForList(GET_DEVICES_GUIDS_AND_OFFLINE_TIMEOUT, parameters);
        results.stream().forEach(map -> deviceInfo.put((String) map.get("guid"), (Integer) map.get("offline_timeout")));
        return deviceInfo;
    }


    @Override
    public Device findByUUID(String uuid) {
        return createNamedQuery(Device.class, "Device.findByUUID", Optional.of(CacheConfig.refresh()))
                .setParameter("guid", uuid)
                .getResultList()
                .stream().findFirst().orElse(null);
    }

    @Override
    public void persist(Device device) {
        super.persist(device);
    }

    @Override
    public int deleteByUUID(String guid) {
        return createNamedQuery("Device.deleteByUUID", Optional.<CacheConfig>empty())
                .setParameter("guid", guid)
                .executeUpdate();
    }

    @Override
    public List<Device> getDeviceList(List<String> guids, HivePrincipal principal) {
        final CriteriaBuilder cb = criteriaBuilder();
        final CriteriaQuery<Device> criteria = cb.createQuery(Device.class);
        final Root<Device> from = criteria.from(Device.class);
        final Predicate[] predicates = CriteriaHelper.deviceListPredicates(cb, from, guids, Optional.ofNullable(principal));
        criteria.where(predicates);
        final TypedQuery<Device> query = createQuery(criteria);
        CacheHelper.cacheable(query);
        return query.getResultList();
    }

    @Override
    public long getAllowedDeviceCount(HivePrincipal principal, List<String> guids) {
        final CriteriaBuilder cb = criteriaBuilder();
        final CriteriaQuery<Device> criteria = cb.createQuery(Device.class);
        final Root<Device> from = criteria.from(Device.class);
        final Predicate[] predicates = CriteriaHelper.deviceListPredicates(cb, from, guids, Optional.ofNullable(principal));
        criteria.where(predicates);
        final TypedQuery<Device> query = createQuery(criteria);
        return query.getResultList().size();
    }

    @Override
    public List<Device> getList(String name, String namePattern, String status, Long networkId, String networkName, Long deviceClassId, String deviceClassName, String deviceClassVersion, String sortField, @NotNull Boolean sortOrderAsc, Integer take, Integer skip, HivePrincipal principal) {
        final CriteriaBuilder cb = criteriaBuilder();
        final CriteriaQuery<Device> criteria = cb.createQuery(Device.class);
        final Root<Device> from = criteria.from(Device.class);

        final Predicate [] predicates = CriteriaHelper.deviceListPredicates(cb, from, ofNullable(name), ofNullable(namePattern),
                ofNullable(status), ofNullable(networkId), ofNullable(networkName), ofNullable(deviceClassId),
                ofNullable(deviceClassName), ofNullable(deviceClassVersion), ofNullable(principal));

        criteria.where(predicates);
        CriteriaHelper.order(cb, criteria, from, ofNullable(sortField), sortOrderAsc);

        final TypedQuery<Device> query = createQuery(criteria);
        cacheQuery(query, of(CacheConfig.refresh()));
        ofNullable(take).ifPresent(query::setMaxResults);
        ofNullable(skip).ifPresent(query::setFirstResult);
        return query.getResultList();
    }
}
