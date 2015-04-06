package com.devicehive.utils.mapper;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.exceptions.DriverException;
import com.devicehive.domain.DeviceCommand;
import org.springframework.cassandra.core.RowMapper;

/**
 * Created by tmatvienko on 2/13/15.
 */
public class CommandRowMapper implements RowMapper<DeviceCommand> {
    @Override
    public DeviceCommand mapRow(Row row, int i) throws DriverException {
        DeviceCommand command = new DeviceCommand();
        command.setId(row.getString("id"));
        command.setDeviceGuid(row.getString("device_guid"));
        command.setTimestamp(row.getDate("timestamp"));
        command.setCommand(row.getString("command"));
        command.setParameters(row.getString("parameters"));
        command.setUserId(row.getString("userId"));
        command.setLifetime(row.getInt("lifetime"));
        command.setFlags(row.getInt("flags"));
        command.setStatus(row.getString("status"));
        command.setResult(row.getString("result"));
        command.setIsUpdated(row.getBool("updated"));
        return command;
    }
}
