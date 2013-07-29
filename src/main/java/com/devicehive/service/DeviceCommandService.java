package com.devicehive.service;

import com.devicehive.dao.DeviceCommandDAO;
import com.devicehive.model.DeviceCommand;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.UUID;

/**
 * @author: Nikolay Loboda
 * @since 25.07.13
 */
@Stateless
public class DeviceCommandService {
    @Inject
    private DeviceCommandDAO commandDAO;

    public DeviceCommand getWithDevice(@NotNull long id) {
        return commandDAO.getWithDevice(id);
    }

    public DeviceCommand getWithDeviceAndUser(@NotNull long id) {
        return commandDAO.getWithDeviceAndUser(id);
    }

    public DeviceCommand getByGuidAndId(@NotNull UUID guid, @NotNull long id){
        return commandDAO.getByDeviceGuidAndId(guid,id);
    }
}
