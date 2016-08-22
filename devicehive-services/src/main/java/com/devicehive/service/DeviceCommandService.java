package com.devicehive.service;

import com.devicehive.messages.bus.MessageBus;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.wrappers.DeviceCommandWrapper;
import com.devicehive.service.time.TimestampService;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.Random;


@Service
public class DeviceCommandService {

    @Autowired
    private TimestampService timestampService;

    @Autowired
    private HiveValidator hiveValidator;

    @Autowired
    private HazelcastService hazelcastService;

    @Deprecated
    @Autowired
    private MessageBus messageBus;

    public Optional<DeviceCommand> find(Long id, String guid) {
        return hazelcastService.find(id, guid, DeviceCommand.class);
    }

    public Collection<DeviceCommand> find(Collection<String> devices, Collection<String> names,
                                          Date timestamp, String status, Integer take,
                                          Boolean hasResponse) {
        return hazelcastService.find(devices, names, timestamp, status, take, hasResponse, DeviceCommand.class);
    }

    public DeviceCommand insert(DeviceCommandWrapper commandWrapper, DeviceVO device, UserVO user) {
        DeviceCommand command = new DeviceCommand();
        command.setId(Math.abs(new Random().nextInt()));
        command.setDeviceGuid(device.getGuid());
        command.setIsUpdated(false);
        command.setTimestamp(timestampService.getTimestamp());

        if(user != null){
            command.setUserId(user.getId());
        }
        if (commandWrapper.getCommand() != null) {
            command.setCommand(commandWrapper.getCommand().orElseGet(null));
        }
        if (commandWrapper.getParameters() != null) {
            command.setParameters(commandWrapper.getParameters().orElse(null));
        }
        if (commandWrapper.getLifetime() != null) {
            command.setLifetime(commandWrapper.getLifetime().orElse(null));
        }
        if (commandWrapper.getStatus() != null) {
            command.setStatus(commandWrapper.getStatus().orElse(null));
        }
        if (commandWrapper.getResult() != null) {
            command.setResult(commandWrapper.getResult().orElse(null));
        }

        hiveValidator.validate(command);
        store(command);
        return command;
    }

    public void update(Long commandId, String deviceGuid, DeviceCommandWrapper commandWrapper){
        // TODO: [asuprun] handle case when command not found
        DeviceCommand command = find(commandId, deviceGuid).get();
        command.setIsUpdated(true);

        if (commandWrapper.getCommand() != null) {
            command.setCommand(commandWrapper.getCommand().orElse(null));
        }
        if (commandWrapper.getParameters() != null) {
            command.setParameters(commandWrapper.getParameters().orElse(null));
        }
        if (commandWrapper.getLifetime() != null) {
            command.setLifetime(commandWrapper.getLifetime().orElse(null));
        }
        if (commandWrapper.getStatus() != null) {
            command.setStatus(commandWrapper.getStatus().orElse(null));
        }
        if (commandWrapper.getResult() != null) {
            command.setResult(commandWrapper.getResult().orElse(null));
        }

        hiveValidator.validate(command);
        store(command);
    }

    public void store(DeviceCommand command) {
        hazelcastService.store(command, DeviceCommand.class);
        messageBus.publish(command);
    }
}
