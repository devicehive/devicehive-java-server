package com.devicehive.service;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.messages.bus.MessageBus;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.User;
import com.devicehive.model.wrappers.DeviceCommandWrapper;
import com.devicehive.service.time.TimestampService;
import com.devicehive.util.HiveValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
public class DeviceCommandService extends AbstractHazelcastEntityService {
    @Autowired
    private TimestampService timestampService;
    @Autowired
    private HiveValidator hiveValidator;
    @Autowired
    private MessageBus messageBus;

    public DeviceCommand find(Long id, String guid) {
        return find(id, guid, DeviceCommand.class);
    }

    public Collection<DeviceCommand> find(Collection<String> devices, Collection<String> names,
                                          Date timestamp, String status, Integer take,
                                          Boolean isUpdated, HivePrincipal principal) {
        return find(devices, names, timestamp, status, take, isUpdated, principal, DeviceCommand.class);
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

    public void store(DeviceCommand command) {
        command.setIsUpdated(false);
        store(command, DeviceCommand.class);
    }

    //FIXME: temporary added, need to understand necessity of this method
    public void submitDeviceCommandUpdate(DeviceCommand command) {
        final DeviceCommand existing = find(command.getId(), command.getDeviceGuid());
        if(existing != null) {
            if(command.getCommand() == null) {
                command.setCommand(existing.getCommand());
            }
            command.setIsUpdated(true);
            store(command);
        }

        messageBus.publishDeviceCommandUpdate(command);
    }
}
