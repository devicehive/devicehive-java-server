package com.devicehive.utils.mapper;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.exceptions.DriverException;
import com.devicehive.domain.DeviceNotification;
import org.springframework.cassandra.core.RowMapper;

/**
 * Created by tatyana on 2/11/15.
 */
public class NotificationRowMapper implements RowMapper<DeviceNotification> {

    @Override
    public DeviceNotification mapRow(Row row, int i) throws DriverException {
        DeviceNotification notificationWrapper = new DeviceNotification();
        notificationWrapper.setId(row.getString("id"));
        notificationWrapper.setDeviceGuid(row.getString("device_guid"));
        notificationWrapper.setTimestamp(row.getDate("timestamp"));
        notificationWrapper.setNotification(row.getString("notification"));
        notificationWrapper.setParameters(row.getString("parameters"));
        return notificationWrapper;
    }
}
