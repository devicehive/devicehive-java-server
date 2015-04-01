package com.devicehive.utils.mapper;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.exceptions.DriverException;
import com.devicehive.domain.wrappers.JsonStringWrapper;
import com.devicehive.domain.wrappers.DeviceCommandWrapper;
import org.springframework.cassandra.core.RowMapper;

import java.sql.Timestamp;

/**
 * Created by tmatvienko on 2/13/15.
 */
public class CommandRowMapper implements RowMapper<DeviceCommandWrapper> {
    @Override
    public DeviceCommandWrapper mapRow(Row row, int i) throws DriverException {
        DeviceCommandWrapper commandWrapper = new DeviceCommandWrapper();
        commandWrapper.setId(Long.parseLong(row.getString("id")));
        commandWrapper.setDeviceGuid(row.getString("device_guid"));
        if (row.getDate("timestamp") != null) {
            commandWrapper.setTimestamp(new Timestamp(row.getDate("timestamp").getTime()));
        }
        if (row.getString("command") != null) {
            commandWrapper.setCommand(row.getString("command"));
        }
        if (row.getString("parameters") != null) {
            commandWrapper.setParameters(new JsonStringWrapper(row.getString("parameters")));
        }
        if (row.getString("userId") != null) {
            commandWrapper.setUserId(Long.parseLong(row.getString("userId")));
        }
        if (row.getInt("lifetime") != 0) {
            commandWrapper.setLifetime(row.getInt("lifetime"));
        }
        if (row.getInt("flags") != 0) {
            commandWrapper.setFlags(row.getInt("flags"));
        }
        if (row.getString("status") != null) {
            commandWrapper.setStatus(row.getString("status"));
        }
        if (row.getString("result") != null) {
            commandWrapper.setResult(new JsonStringWrapper(row.getString("result")));
        }
        commandWrapper.setIsUpdated(row.getBool("updated"));
        return commandWrapper;
    }
}
