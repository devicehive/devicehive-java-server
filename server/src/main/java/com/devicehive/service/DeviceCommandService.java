package com.devicehive.service;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.messages.bus.Create;
import com.devicehive.messages.bus.Update;
import com.devicehive.messages.bus.redis.RedisCommandService;
import com.devicehive.messages.kafka.Command;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.User;
import com.devicehive.model.wrappers.DeviceCommandWrapper;
import com.devicehive.util.HiveValidator;
import com.devicehive.util.LogExecutionTime;
import org.apache.commons.collections.CollectionUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.util.*;


@Stateless
@LogExecutionTime
public class DeviceCommandService {
    private static final int MAX_COMMAND_COUNT = 100;

    @EJB
    private TimestampService timestampService;
    @EJB
    private DeviceService deviceService;
    @EJB
    private HiveValidator hiveValidator;
    @EJB
    private RedisCommandService redisCommandService;

    @Inject
    @Command
    @Create
    private Event<DeviceCommand> deviceCommandMessageReceivedEvent;

    @Inject
    @Command
    @Update
    private Event<DeviceCommand> deviceCommandUpdateMessageReceivedEvent;

    public DeviceCommand findByIdAndGuid(final Long id, final String guid) {
        return redisCommandService.getByIdAndGuid(id, guid);
    }

    public Collection<DeviceCommand> getDeviceCommandsList(Collection<String> devices, final Collection<String> names,
                                                     final Timestamp timestamp, final Boolean isUpdated, HivePrincipal principal) {
        Collection<DeviceCommand> commands;
        if (devices != null) {
            final List<String> availableDevices = deviceService.findGuidsWithPermissionsCheck(devices, principal);
            commands = redisCommandService.getByGuids(availableDevices, names, timestamp, isUpdated);
        } else {
            commands = redisCommandService.getAll(names, timestamp, isUpdated);
        }
        if (CollectionUtils.isNotEmpty(commands) && commands.size() > MAX_COMMAND_COUNT) {
            return new ArrayList<DeviceCommand>(commands).subList(0, MAX_COMMAND_COUNT);
        }
        return commands;
    }

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

    public void submitDeviceCommand(DeviceCommand message) {
        message.setIsUpdated(false);
        redisCommandService.save(message);
        deviceCommandMessageReceivedEvent.fire(message);
    }

    public void submitDeviceCommandUpdate(DeviceCommand message) {
        message.setIsUpdated(true);
        redisCommandService.update(message);
        deviceCommandUpdateMessageReceivedEvent.fire(message);
    }

    public void submitEmptyResponse(final AsyncResponse asyncResponse) {
        asyncResponse.resume(ResponseFactory.response(Response.Status.OK, Collections.emptyList(), JsonPolicyDef.Policy.COMMAND_LISTED));
    }
}
