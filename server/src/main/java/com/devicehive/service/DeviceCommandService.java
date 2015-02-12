package com.devicehive.service;

import com.datastax.driver.core.utils.UUIDs;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.dao.DeviceCommandDAO;
import com.devicehive.messages.bus.Create;
import com.devicehive.messages.bus.GlobalMessage;
import com.devicehive.messages.bus.LocalMessage;
import com.devicehive.messages.bus.Update;
import com.devicehive.messages.kafka.Command;
import com.devicehive.model.*;
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

    public void submitDeviceCommandUpdate(DeviceCommandWrapper update, Device device, User user, String commandId) {
        DeviceCommandMessage message = convertToDeviceCommandMessage(update, device, user, null, commandId);
        deviceCommandUpdateMessageReceivedEvent.fire(message);
    }

    public void submitDeviceCommand(DeviceCommandWrapper command, Device device, User user) {
        DeviceCommandMessage message = convertToDeviceCommandMessage(command, device, user, null, UUIDs.timeBased().toString());
        deviceCommandMessageReceivedEvent.fire(message);
    }

    public void submitDeviceCommand(DeviceCommandWrapper command, Device device, User user, String sessionId) {
        DeviceCommandMessage message = convertToDeviceCommandMessage(command, device, user, sessionId, UUIDs.timeBased().toString());
        deviceCommandMessageReceivedEvent.fire(message);
    }

    private DeviceCommandMessage convertToDeviceCommandMessage(DeviceCommandWrapper command, Device device, User user,
                                                              String sessionId, String commandId) {

        DeviceCommandMessage message = new DeviceCommandMessage();
        message.setId(commandId);
        message.setDeviceGuid(device.getGuid());
        message.setTimestamp(timestampService.getTimestamp());
        message.setUserId(user.getId());
        message.setCommand(command.getCommand());
        if (command.getParameters() != null) {
            message.setParameters(command.getParameters().getJsonString());
        }
        if (command.getLifetime() != null) {
            message.setLifetime(command.getLifetime());
        }
        if (command.getStatus() != null) {
            message.setStatus(command.getStatus());
        }
        if (command.getResult() != null) {
            message.setResult(command.getResult().getJsonString());
        }
        if (sessionId != null) {
            message.setOriginSessionId(sessionId);
        }
        hiveValidator.validate(message);
        return message;
    }


}
