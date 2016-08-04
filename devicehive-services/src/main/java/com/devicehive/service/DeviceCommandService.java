package com.devicehive.service;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.User;
import com.devicehive.model.wrappers.DeviceCommandWrapper;
import com.devicehive.service.time.TimestampService;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.DeviceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Date;
import java.util.Random;


@Service
public class DeviceCommandService extends AbstractHazelcastEntityService {
    @Autowired
    private TimestampService timestampService;
    @Autowired
    private HiveValidator hiveValidator;

    public DeviceCommand find(Long id, String guid) {
        return find(id, guid, DeviceCommand.class);
    }

    public Collection<DeviceCommand> find(Collection<String> devices, Collection<String> names,
                                          Date timestamp, String status, Integer take,
                                          Boolean hasResponse, HivePrincipal principal) {
        return find(devices, names, timestamp, status, take, hasResponse, principal, DeviceCommand.class);
    }

    public DeviceCommand insert(DeviceCommandWrapper commandWrapper, DeviceVO device, User user) {
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
        DeviceCommand command = find(commandId, deviceGuid);
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
        store(command, DeviceCommand.class);
    }
}
