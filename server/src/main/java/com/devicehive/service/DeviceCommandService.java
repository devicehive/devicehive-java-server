package com.devicehive.service;

import com.datastax.driver.core.utils.UUIDs;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.dao.DeviceCommandDAO;
import com.devicehive.messages.bus.Create;
import com.devicehive.messages.bus.GlobalMessage;
import com.devicehive.messages.bus.LocalMessage;
import com.devicehive.messages.bus.Update;
import com.devicehive.messages.kafka.Command;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceCommandMessage;
import com.devicehive.model.User;
import com.devicehive.model.updates.DeviceCommandUpdateMessage;
import com.devicehive.util.HiveValidator;
import com.devicehive.util.LogExecutionTime;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;


@Stateless
@LogExecutionTime
public class DeviceCommandService {

    @EJB
    private DeviceCommandDAO commandDAO;
    @EJB
    private DeviceService deviceService;
    @EJB
    private TimestampService timestampService;
    @EJB
    private HiveValidator hiveValidator;

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

    @Inject
    @Command
    @Create
    private Event<DeviceCommandMessage> deviceCommandMessageReceivedEvent;

    @Inject
    @Command
    @Update
    private Event<DeviceCommandMessage> deviceCommandUpdateMessageReceivedEvent;


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

    public void submitDeviceCommandUpdate(DeviceCommandUpdateMessage update, Device device) {
        DeviceCommandMessage saved = saveDeviceCommandUpdate(update, device);
        deviceCommandUpdateMessageReceivedEvent.fire(saved);
    }

    public void submitDeviceCommand(DeviceCommandMessage command, Device device, User user) {
        command.setId(UUIDs.timeBased().timestamp());
        command.setDeviceGuid(device.getGuid());
        command.setUserId(user.getId());
        command.setTimestamp(timestampService.getTimestamp());
        deviceCommandMessageReceivedEvent.fire(command);
    }

    private DeviceCommandMessage saveDeviceCommandUpdate(DeviceCommandUpdateMessage update, Device device) {

        //TODO: implement updateing an exidting DeviceCommand object
        //DeviceCommand cmd = commandDAO.findById(update.getId());
        DeviceCommandMessage cmd = new DeviceCommandMessage();
        cmd.setId(update.getId());

//        if (cmd == null) {
//            throw new HiveException(String.format(Messages.COMMAND_NOT_FOUND, update.getId()),
//                                    NOT_FOUND.getStatusCode());
//        }
        cmd.setDeviceGuid(device.getGuid());

//        if (!cmd.getDevice().getId().equals(device.getId())) {
//            throw new HiveException(String.format(Messages.COMMAND_NOT_FOUND, update.getId()),
//                                    NOT_FOUND.getStatusCode());
//        }

        if (update.getCommand() != null) {
            cmd.setCommand(update.getCommand());
        }
        if (update.getFlags() != null) {
            cmd.setFlags(update.getFlags());
        }
        if (update.getLifetime() != null) {
            cmd.setLifetime(update.getLifetime());
        }
        if (update.getParameters() != null) {
            cmd.setParameters(update.getParameters());
        }
        if (update.getResult() != null) {
            cmd.setResult(update.getResult());
        }
        if (update.getStatus() != null) {
            cmd.setStatus(update.getStatus());
        }
        if (update.getTimestamp() != null) {
            cmd.setTimestamp(update.getTimestamp());
        }
        hiveValidator.validate(cmd);
        return cmd;
    }


}
