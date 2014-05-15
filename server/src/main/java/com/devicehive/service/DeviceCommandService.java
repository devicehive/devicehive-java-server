package com.devicehive.service;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.DeviceCommandDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.messages.bus.GlobalMessageBus;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.User;
import com.devicehive.model.updates.DeviceCommandUpdate;
import com.devicehive.util.LogExecutionTime;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;


@Stateless
@LogExecutionTime
public class DeviceCommandService {

    private DeviceCommandDAO commandDAO;
    private DeviceCommandService self;
    private GlobalMessageBus globalMessageBus;
    private DeviceService deviceService;


    @EJB
    public void setGlobalMessageBus(GlobalMessageBus globalMessageBus) {
        this.globalMessageBus = globalMessageBus;
    }

    @EJB
    public void setSelf(DeviceCommandService self) {
        this.self = self;
    }

    @EJB
    public void setCommandDAO(DeviceCommandDAO commandDAO) {
        this.commandDAO = commandDAO;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceCommand getByGuidAndId(@NotNull String guid, @NotNull long id) {
        return commandDAO.getByDeviceGuidAndId(guid, id);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceCommand findById(Long id) {
        return commandDAO.findById(id);
    }

    @EJB
    public void setDeviceService(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceCommand> getDeviceCommandsList(Collection<String> devices, Collection<String> names,
                                                     Timestamp timestamp,
                                                     HivePrincipal principal) {
        if (devices != null) {
            return commandDAO.findCommands(deviceService.findByGuidWithPermissionsCheck(devices, principal), names, timestamp, null);
        } else {
            return commandDAO.findCommands(null, names, timestamp, principal);
        }
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceCommand> queryDeviceCommand(Device device, Timestamp start, Timestamp end, String command,
                                                  String status, String sortField, Boolean sortOrderAsc,
                                                  Integer take, Integer skip, Integer gridInterval) {
        return commandDAO.queryDeviceCommand(device, start, end, command, status, sortField, sortOrderAsc, take,
                skip, gridInterval);
    }

    public DeviceCommand getByDeviceGuidAndId(@NotNull String guid, @NotNull long id) {
        return commandDAO.getByDeviceGuidAndId(guid, id);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void submitDeviceCommandUpdate(DeviceCommandUpdate update, Device device) {
        DeviceCommand saved = self.saveDeviceCommandUpdate(update, device);
        globalMessageBus.publishDeviceCommandUpdate(saved);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void submitDeviceCommand(DeviceCommand command, Device device, User user) {
        self.saveDeviceCommand(command, device, user);
        globalMessageBus.publishDeviceCommand(command);
    }

    public void saveDeviceCommand(final DeviceCommand command, Device device, User user) {
        command.setDevice(device);
        command.setUser(user);
        commandDAO.createCommand(command);
    }

    public DeviceCommand saveDeviceCommandUpdate(DeviceCommandUpdate update, Device device) {

        DeviceCommand cmd = commandDAO.findById(update.getId());

        if (cmd == null) {
            throw new HiveException(String.format(Messages.COMMAND_NOT_FOUND, update.getId()), NOT_FOUND.getStatusCode());
        }

        if (!cmd.getDevice().getId().equals(device.getId())) {
            throw new HiveException(String.format(Messages.COMMAND_NOT_FOUND, update.getId()), NOT_FOUND.getStatusCode());
        }


        if (update.getCommand() != null) {
            cmd.setCommand(update.getCommand().getValue());
        }
        if (update.getFlags() != null) {
            cmd.setFlags(update.getFlags().getValue());
        }
        if (update.getLifetime() != null) {
            cmd.setLifetime(update.getLifetime().getValue());
        }
        if (update.getParameters() != null) {
            cmd.setParameters(update.getParameters().getValue());
        }
        if (update.getResult() != null) {
            cmd.setResult(update.getResult().getValue());
        }
        if (update.getStatus() != null) {
            cmd.setStatus(update.getStatus().getValue());
        }
        if (update.getTimestamp() != null) {
            cmd.setTimestamp(update.getTimestamp().getValue());
        }
        return cmd;
    }

}
