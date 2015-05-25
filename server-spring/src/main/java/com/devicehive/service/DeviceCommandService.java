package com.devicehive.service;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.messages.bus.MessageBus;
import com.devicehive.messages.bus.redis.RedisCommandService;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.User;
import com.devicehive.model.wrappers.DeviceCommandWrapper;
import com.devicehive.util.HiveValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.util.*;


@Component
public class DeviceCommandService {
    private static final int MAX_COMMAND_COUNT = 100;

    @Autowired
    private TimestampService timestampService;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private HiveValidator hiveValidator;
    @Autowired
    private RedisCommandService redisCommandService;
    @Autowired
    private MessageBus messageBus;

    public DeviceCommand findByIdAndGuid(final Long id, final String guid) {
        return redisCommandService.getByIdAndGuid(id, guid);
    }

    public Collection<DeviceCommand> getDeviceCommandsList(Collection<String> devices, final Collection<String> names,
                                                     final Timestamp timestamp, final String status, final Integer take, final Boolean isUpdated, HivePrincipal principal) {
        Collection<DeviceCommand> commands;
        if (devices != null) {
            final List<String> availableDevices = deviceService.findGuidsWithPermissionsCheck(devices, principal);
            commands = redisCommandService.getByGuids(availableDevices, names, timestamp, status, take, isUpdated);
        } else {
            commands = redisCommandService.getAll(names, timestamp, status, take, isUpdated);
        }
        if (!CollectionUtils.isEmpty(commands) && commands.size() > MAX_COMMAND_COUNT) {
            return new ArrayList<>(commands).subList(0, MAX_COMMAND_COUNT);
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
        messageBus.publishDeviceCommand(message);
    }

    public void submitDeviceCommandUpdate(DeviceCommand message) {
        message.setIsUpdated(true);
        redisCommandService.update(message);
        messageBus.publishDeviceCommandUpdate(message);
    }

    public void submitEmptyResponse(final AsyncResponse asyncResponse) {
        asyncResponse.resume(ResponseFactory.response(Response.Status.OK, Collections.emptyList(), JsonPolicyDef.Policy.COMMAND_LISTED));
    }
}
