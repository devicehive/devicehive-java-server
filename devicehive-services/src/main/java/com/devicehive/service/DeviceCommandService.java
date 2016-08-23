package com.devicehive.service;

import com.devicehive.messages.bus.MessageBus;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.rpc.CommandSearchRequest;
import com.devicehive.model.rpc.CommandSearchResponse;
import com.devicehive.model.rpc.NotificationSearchRequest;
import com.devicehive.model.wrappers.DeviceCommandWrapper;
import com.devicehive.service.time.TimestampService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.UserVO;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@Service
public class DeviceCommandService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceNotificationService.class);

    @Autowired
    private TimestampService timestampService;

    @Autowired
    private HiveValidator hiveValidator;

    @Autowired
    private RpcClient rpcClient;

    @Autowired
    private Gson gson;

    @Deprecated
    @Autowired
    private MessageBus messageBus;

    @SuppressWarnings("unchecked")
    public Optional<DeviceCommand> find(Long id, String guid) {
        CompletableFuture<Response> future = new CompletableFuture<>();
        rpcClient.call(Request.newBuilder()
                .withCorrelationId(UUID.randomUUID().toString())
                .withBody(new CommandSearchRequest() {{
                    setId(id);
                    setGuid(guid);
                }})
                .build(), future::complete);
        try {
            Response response = future.get(10, TimeUnit.SECONDS);
            return ((CommandSearchResponse) response.getBody()).getCommands().stream().findFirst();
        } catch (InterruptedException | ExecutionException e) {
            logger.warn("Unable to find command due to unexpected exception", e);
        } catch (TimeoutException e) {
            logger.warn("Command find was timed out (id={}, guid={})", id, guid, e);
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public Collection<DeviceCommand> find(Collection<String> devices, Collection<String> names,
                                          Date timestamp, String status, Integer take, Boolean hasResponse) {
        CompletableFuture<Response> future = new CompletableFuture<>();
        rpcClient.call(Request.newBuilder()
                .withCorrelationId(UUID.randomUUID().toString())
                .withBody(new CommandSearchRequest() {{
                    setDevices(new HashSet<>(devices));
                    setDevices(new HashSet<>(names));
                    setTimestamp(timestamp);
                    setStatus(status);
                    setTake(take);
                    setHasResponse(hasResponse);
                }})
                .build(), future::complete);
        try {
            Response response = future.get(10, TimeUnit.SECONDS);
            return ((CommandSearchResponse) response.getBody()).getCommands();
        } catch (InterruptedException | ExecutionException e) {
            logger.warn("Unable to find command due to unexpected exception", e);
        } catch (TimeoutException e) {
            logger.warn("Commands find was timed out", e);
        }
        return Collections.emptyList();
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

    protected void store(DeviceCommand command) {
//        hazelcastService.store(command, DeviceCommand.class); // FIXME: implement
        messageBus.publish(command);
    }
}
