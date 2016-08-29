package com.devicehive.service;

import com.devicehive.messages.bus.MessageBus;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.rpc.CommandSearchRequest;
import com.devicehive.model.rpc.CommandSearchResponse;
import com.devicehive.model.wrappers.DeviceCommandWrapper;
import com.devicehive.service.time.TimestampService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.UserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@Service
public class DeviceCommandService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceNotificationService.class);

    @Autowired
    private TimestampService timestampService;

    @Autowired
    private HiveValidator hiveValidator;

    @Autowired
    private RpcClient rpcClient;

    @Deprecated
    @Autowired
    private MessageBus messageBus;

    @SuppressWarnings("unchecked")
    public CompletableFuture<Optional<DeviceCommand>> find(Long id, String guid) {
        CommandSearchRequest searchRequest = new CommandSearchRequest();
        searchRequest.setId(id);
        searchRequest.setGuid(guid);

        CompletableFuture<Response> future = new CompletableFuture<>();
        rpcClient.call(Request.newBuilder()
                .withCorrelationId(UUID.randomUUID().toString())
                .withBody(searchRequest)
                .build(), future::complete); // TODO: complete future conditionally according to com.devicehive.shim.api.Response.isFailed()
        return future.thenApply(r -> ((CommandSearchResponse) r.getBody()).getCommands().stream().findFirst());
    }

    @SuppressWarnings("unchecked")
    public CompletableFuture<List<DeviceCommand>> find(Collection<String> guids, Collection<String> names,
                                                       Date timestamp, String status, Integer take, Boolean hasResponse) {
        List<CompletableFuture<Response>> futures = guids.stream()
                .map(guid -> {
                    CommandSearchRequest searchRequest = new CommandSearchRequest();
                    searchRequest.setGuid(guid);
                    searchRequest.setNames(new HashSet<>(names));
                    searchRequest.setTimestamp(timestamp);
                    searchRequest.setStatus(status);
                    searchRequest.setTake(take);
                    searchRequest.setHasResponse(hasResponse);
                    return searchRequest;
                })
                .map(searchRequest -> {
                    CompletableFuture<Response> future = new CompletableFuture<>();
                    rpcClient.call(Request.newBuilder()
                            .withBody(searchRequest)
                            .withPartitionKey(searchRequest.getGuid())
                            .build(), future::complete); // TODO: complete future conditionally according to com.devicehive.shim.api.Response.isFailed()
                    return future;
                })
                .collect(Collectors.toList());

        // List<CompletableFuture<Response>> => CompletableFuture<List<DeviceCommand>>
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)                                  // List<CompletableFuture<Response>> => CompletableFuture<List<Response>>
                        .map(r -> ((CommandSearchResponse) r.getBody()).getCommands()) // CompletableFuture<List<Response>> => CompletableFuture<List<List<DeviceCommand>>>
                        .flatMap(Collection::stream)                                   // CompletableFuture<List<List<DeviceCommand>>> => CompletableFuture<List<DeviceCommand>>
                        .collect(Collectors.toList()));
    }

    public DeviceCommand insert(DeviceCommandWrapper commandWrapper, DeviceVO device, UserVO user) {
        DeviceCommand command = new DeviceCommand();
        command.setId(Math.abs(new Random().nextInt()));
        command.setDeviceGuid(device.getGuid());
        command.setIsUpdated(false);
        command.setTimestamp(timestampService.getTimestamp());

        if (user != null) {
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

    public void update(Long commandId, String deviceGuid, DeviceCommandWrapper commandWrapper) {
        // TODO: [asuprun] handle case when command not found
        DeviceCommand command = find(commandId, deviceGuid).join().get();
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
