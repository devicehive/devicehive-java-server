package com.devicehive.service;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.dao.DeviceCommandDAO;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.dao.UserDAO;
import com.devicehive.model.*;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import java.util.Set;
import java.util.UUID;

/**
 * @author: Nikolay Loboda
 * @since 25.07.13
 */
@Stateless
public class DeviceCommandService {
    @Inject
    private DeviceCommandDAO commandDAO;

    @Inject
    private UserDAO userDAO;

    @Inject
    private DeviceDAO deviceDAO;

    @Context
    private ContainerRequestContext requestContext;

    public DeviceCommand getWithDevice(@NotNull long id) {
        return commandDAO.getWithDevice(id);
    }

    public DeviceCommand getWithDeviceAndUser(@NotNull long id) {
        return commandDAO.getWithDeviceAndUser(id);
    }

    public DeviceCommand getByGuidAndId(@NotNull UUID guid, @NotNull long id){
        return commandDAO.getByDeviceGuidAndId(guid,id);
    }

    public Device getDevice(String uuid) {
        UUID deviceId;

        try {
            deviceId = UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("unparseable guid: '" + uuid + "'");
        }

        Device device = deviceDAO.findByUUID(deviceId);

        if (device == null) {
            throw new NotFoundException("device with guid " + uuid + " not found");
        }

        return device;
    }

    public boolean checkPermissions(Device device) {
        HivePrincipal principal = (HivePrincipal) requestContext.getSecurityContext().getUserPrincipal();
        if (principal.getDevice() != null) {

            if (!device.getGuid().equals(principal.getDevice().getGuid())) {
                return false;
            }

            if (device.getNetwork() == null) {
                return false;
            }

        } else {
            User user = principal.getUser();
            if (user.getRole().equals(UserRole.CLIENT)) {
                User userWithNetworks = userDAO.findUserWithNetworks(user.getId());
                Set<Network> networkSet = userWithNetworks.getNetworks();
                return networkSet.contains(device.getNetwork());
            }
        }
        return true;
    }
}
