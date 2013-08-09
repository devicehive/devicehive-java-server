package com.devicehive.service;

import com.devicehive.dao.DeviceCommandDAO;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * @author: Nikolay Loboda
 * @since 25.07.13
 */
@Stateless
public class DeviceCommandService {
    @EJB
    private DeviceCommandDAO commandDAO;

    public DeviceCommand getWithDevice(@NotNull long id) {
        return commandDAO.getWithDevice(id);
    }

    public DeviceCommand getWithDeviceAndUser(@NotNull long id) {
        return commandDAO.getWithDeviceAndUser(id);
    }

    public DeviceCommand getByGuidAndId(@NotNull UUID guid, @NotNull long id) {
        return commandDAO.getByDeviceGuidAndId(guid, id);
    }

    public DeviceCommand findById(Long id) {
        return commandDAO.findById(id);
    }

    public List<DeviceCommand> queryDeviceCommand(Device device, Timestamp start, Timestamp end, String command,
                                                  String status, String sortField, Boolean sortOrderAsc,
                                                  Integer take, Integer skip) {
        return commandDAO.queryDeviceCommand(device, start, end, command, status, sortField, sortOrderAsc, take, skip);
    }

    public DeviceCommand getByDeviceGuidAndId(@NotNull UUID guid, @NotNull long id) {
        return commandDAO.getByDeviceGuidAndId(guid, id);
    }
}
