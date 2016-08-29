package com.devicehive.service;

import com.devicehive.json.adapters.TimestampAdapter;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.rpc.CommandInsertRequest;
import com.devicehive.model.wrappers.DeviceCommandWrapper;
import com.devicehive.service.time.TimestampService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.util.HiveValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Consumer;

@Lazy
@Component
public class DeviceCommandServiceImpl {

    @Autowired
    private RpcClient rpcClient;

    @Autowired
    private HiveValidator hiveValidator;

    public void insert(DeviceCommand command, Consumer<Response> callback) {
        CommandInsertRequest requestBody = new CommandInsertRequest(command);
        Request request = Request.newBuilder()
                .withPartitionKey(command.getDeviceGuid())
                .withCorrelationId(UUID.randomUUID().toString())
                .withBody(requestBody)
                .build();

        rpcClient.call(request, callback);
    }

    public DeviceCommand createCommand(DeviceCommandWrapper commandWrapper, String deviceGuid, Long userId) {
        DeviceCommand command = new DeviceCommand();
        command.setId(Math.abs(new Random().nextInt()));
        command.setDeviceGuid(deviceGuid);
        command.setIsUpdated(false);
        command.setTimestamp(new Date());

        if (userId != null) {
            command.setUserId(userId);
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
        return command;
    }
}
