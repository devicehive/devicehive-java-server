package com.devicehive.service;

import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.messages.bus.Create;
import com.devicehive.messages.bus.Update;
import com.devicehive.messages.kafka.Command;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.User;
import com.devicehive.model.wrappers.DeviceCommandWrapper;
import com.devicehive.util.HiveValidator;
import com.devicehive.util.LogExecutionTime;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Random;


@Stateless
@LogExecutionTime
public class DeviceCommandService {

    @EJB
    private TimestampService timestampService;
    @EJB
    private HiveValidator hiveValidator;

    @Inject
    @Command
    @Create
    private Event<DeviceCommand> deviceCommandMessageReceivedEvent;

    @Inject
    @Command
    @Update
    private Event<DeviceCommand> deviceCommandUpdateMessageReceivedEvent;

    public DeviceCommand convertToDeviceCommand(DeviceCommandWrapper commandWrapper, Device device, User user, Long commandId) {
        DeviceCommand command = new DeviceCommand();
        command.setTimestamp(timestampService.getTimestamp());
        if (commandId == null) {
            //TODO: Replace with UUID
            command.setId(Math.abs(new Random().nextInt()));
        } else {
            command.setId(commandId);
        }
        command.setDeviceGuid(device.getGuid());
        command.setCommand(commandWrapper.getCommand());
        if (user != null) {
            command.setUserId(user.getId());
        }
        if (commandWrapper.getParameters() != null) {
            command.setParameters(commandWrapper.getParameters());
        }
        if (commandWrapper.getLifetime() != null) {
            command.setLifetime(commandWrapper.getLifetime());
        }
        if (commandWrapper.getStatus() != null) {
            command.setStatus(commandWrapper.getStatus());
        }
        if (commandWrapper.getResult() != null) {
            command.setResult(commandWrapper.getResult());
        }
        hiveValidator.validate(command);
        return command;
    }

    public void submitDeviceCommandUpdate(DeviceCommand message) {
        deviceCommandUpdateMessageReceivedEvent.fire(message);
    }

    public void submitDeviceCommand(DeviceCommand message) {
        deviceCommandMessageReceivedEvent.fire(message);
    }

    public void submitEmptyResponse(final AsyncResponse asyncResponse) {
        asyncResponse.resume(ResponseFactory.response(Response.Status.OK, Collections.emptyList(), JsonPolicyDef.Policy.COMMAND_LISTED));
    }
}
