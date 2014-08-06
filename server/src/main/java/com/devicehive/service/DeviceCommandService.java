package com.devicehive.service;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.DeviceCommandDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.messages.bus.Create;
import com.devicehive.messages.bus.GlobalMessage;
import com.devicehive.messages.bus.LocalMessage;
import com.devicehive.messages.bus.Update;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.User;
import com.devicehive.model.updates.DeviceCommandUpdate;
import com.devicehive.util.LogExecutionTime;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.validation.constraints.NotNull;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;


@Stateless
@LogExecutionTime
public class DeviceCommandService {

    @EJB
    private DeviceCommandDAO commandDAO;
    @EJB
    private DeviceService deviceService;
    @EJB
    private TimestampService timestampService;

    @Inject
    @Create
    @GlobalMessage
    private Event<DeviceCommand> commandEventGlobal;

    @Inject
    @Create
    @LocalMessage
    private Event<DeviceCommand> commandEventLocal;

    @Inject
    @Update
    @GlobalMessage
    private Event<DeviceCommand> updateEventGlobal;

    @Inject
    @Update
    @LocalMessage
    private Event<DeviceCommand> updateEventLocal;


    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public DeviceCommand getByGuidAndId(@NotNull String guid, @NotNull long id) {
        return commandDAO.getByDeviceGuidAndId(guid, id);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public DeviceCommand findById(Long id) {
        return commandDAO.findById(id);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<DeviceCommand> getDeviceCommandsList(Collection<String> devices, Collection<String> names,
                                                     Timestamp timestamp,
                                                     HivePrincipal principal) {
        if (devices != null) {
            return commandDAO
                    .findCommands(deviceService.findByGuidWithPermissionsCheck(devices, principal), names, timestamp,
                            null);
        } else {
            return commandDAO.findCommands(null, names, timestamp, principal);
        }
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<DeviceCommand> queryDeviceCommand(Device device, Timestamp start, Timestamp end, String command,
                                                  String status, String sortField, Boolean sortOrderAsc,
                                                  Integer take, Integer skip, Integer gridInterval) {
        return commandDAO.queryDeviceCommand(device, start, end, command, status, sortField, sortOrderAsc, take,
                skip, gridInterval);
    }

    public DeviceCommand getByDeviceGuidAndId(@NotNull String guid, @NotNull long id) {
        return commandDAO.getByDeviceGuidAndId(guid, id);
    }

    public void submitDeviceCommandUpdate(DeviceCommandUpdate update, Device device) {
        DeviceCommand saved = saveDeviceCommandUpdate(update, device);
        updateEventGlobal.fire(saved);
        updateEventLocal.fire(saved);
    }

    public void submitDeviceCommand(DeviceCommand command, Device device, User user) {
        command.setDevice(device);
        command.setUser(user);
        command.setUserId(user.getId());
        command.setTimestamp(timestampService.getTimestamp());
        commandDAO.createCommand(command);
        commandEventGlobal.fire(command);
        commandEventLocal.fire(command);
    }

    private DeviceCommand saveDeviceCommandUpdate(DeviceCommandUpdate update, Device device) {

        DeviceCommand cmd = commandDAO.findById(update.getId());

        if (cmd == null) {
            throw new HiveException(String.format(Messages.COMMAND_NOT_FOUND, update.getId()),
                    NOT_FOUND.getStatusCode());
        }

        if (!cmd.getDevice().getId().equals(device.getId())) {
            throw new HiveException(String.format(Messages.COMMAND_NOT_FOUND, update.getId()),
                    NOT_FOUND.getStatusCode());
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
