package com.devicehive.utils.mapper;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.exceptions.DriverException;
import com.devicehive.domain.wrappers.JsonStringWrapper;
import com.devicehive.domain.wrappers.DeviceNotificationWrapper;
import org.springframework.cassandra.core.RowMapper;

import java.sql.Timestamp;

/**
 * Created by tatyana on 2/11/15.
 */
public class NotificationRowMapper implements RowMapper<DeviceNotificationWrapper> {

    @Override
    public DeviceNotificationWrapper mapRow(Row row, int i) throws DriverException {
        DeviceNotificationWrapper notificationWrapper = new DeviceNotificationWrapper();
        notificationWrapper.setId(Long.parseLong(row.getString("id")));
        notificationWrapper.setDeviceGuid(row.getString("device_guid"));
        if (row.getDate("timestamp") != null) {
            notificationWrapper.setTimestamp(new Timestamp(row.getDate("timestamp").getTime()));
        }
        if (row.getString("notification") != null) {
            notificationWrapper.setNotification(row.getString("notification"));
        }
        if (row.getString("parameters") != null) {
            notificationWrapper.setParameters(new JsonStringWrapper(row.getString("parameters")));
        }
        return notificationWrapper;
    }
}
