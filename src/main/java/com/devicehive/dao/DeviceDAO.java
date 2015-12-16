package com.devicehive.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class DeviceDAO {
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


}
