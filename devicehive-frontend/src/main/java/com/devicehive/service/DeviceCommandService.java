package com.devicehive.service;

import com.devicehive.model.DeviceCommand;
import com.devicehive.model.eventbus.events.CommandEvent;
import com.devicehive.model.rpc.*;
import com.devicehive.model.wrappers.DeviceCommandWrapper;
import com.devicehive.service.helpers.ResponseConsumer;
import com.devicehive.service.time.TimestampService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.UserVO;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class DeviceCommandService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceCommandService.class);

    private TimestampService timestampService;
    private HiveValidator hiveValidator;
    private RpcClient rpcClient;

    public DeviceCommandService(TimestampService timestampService,
                                HiveValidator hiveValidator,
                                RpcClient rpcClient) {
        this.timestampService = timestampService;
        this.hiveValidator = hiveValidator;
        this.rpcClient = rpcClient;
    }

    public CompletableFuture<Optional<DeviceCommand>> find(Long id, String guid) {
        CommandSearchRequest searchRequest = new CommandSearchRequest();
        searchRequest.setId(id);
        searchRequest.setGuid(guid);

        CompletableFuture<Response> future = new CompletableFuture<>();
        rpcClient.call(Request.newBuilder()
                .withBody(searchRequest)
                .build(), new ResponseConsumer(future));
        return future.thenApply(r -> r.getBody().cast(CommandSearchResponse.class).getCommands().stream().findFirst());
    }

    public CompletableFuture<List<DeviceCommand>> find(Collection<String> guids, Collection<String> names,
                                                       Date timestampSt, Date timestampEnd, String status) {
        List<CompletableFuture<Response>> futures = guids.stream()
                .map(guid -> {
                    CommandSearchRequest searchRequest = new CommandSearchRequest();
                    searchRequest.setGuid(guid);
                    if (names != null) {
                        searchRequest.setNames(new HashSet<>(names));
                    }
                    searchRequest.setTimestamp(timestamp);
                    searchRequest.setTimestampStart(timestampSt);
                    searchRequest.setTimestampEnd(timestampEnd);
                    searchRequest.setStatus(status);
                    return searchRequest;
                })
                .map(searchRequest -> {
                    CompletableFuture<Response> future = new CompletableFuture<>();
                    rpcClient.call(Request.newBuilder()
                            .withBody(searchRequest)
                            .withPartitionKey(searchRequest.getGuid())
                            .build(), new ResponseConsumer(future));
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

    public CompletableFuture<DeviceCommand> insert(DeviceCommandWrapper commandWrapper, DeviceVO device, UserVO user) {
        DeviceCommand command = convertWrapperToCommand(commandWrapper, device, user);

        CompletableFuture<Response> future = new CompletableFuture<>();
        rpcClient.call(Request.newBuilder()
                .withBody(new CommandInsertRequest(command))
                .build(), new ResponseConsumer(future));
        return future.thenApply(r -> ((CommandInsertResponse) r.getBody()).getDeviceCommand());
    }

    public Pair<String, CompletableFuture<List<DeviceCommand>>> submitCommandSubscribe(
            final Set<String> devices,
            final Set<String> names,
            final Date timestamp,
            final BiConsumer<DeviceCommand, String> callback) throws InterruptedException {

        final String subscriptionId = UUID.randomUUID().toString();
        Collection<CompletableFuture<Collection<DeviceCommand>>> futures = devices.stream()
                .map(device -> new CommandSubscribeRequest(subscriptionId, device, names, timestamp))
                .map(subscribeRequest -> {
                    CompletableFuture<Collection<DeviceCommand>> future = new CompletableFuture<>();
                    Consumer<Response> responseConsumer = response -> {
                        String resAction = response.getBody().getAction();
                        if (resAction.equals(Action.COMMAND_SUBSCRIBE_REQUEST.name())) {
                            future.complete(response.getBody().cast(CommandSubscribeResponse.class).getCommands());
                        } else if (resAction.equals(Action.NOTIFICATION_EVENT.name())) {
                            callback.accept(response.getBody().cast(CommandEvent.class).getCommand(), subscriptionId);
                        } else {
                            logger.warn("Unknown action received from backend {}", resAction);
                        }
                    };
                    Request request = Request.newBuilder()
                            .withBody(subscribeRequest)
                            .withPartitionKey(subscribeRequest.getDevice())
                            .withSingleReply(false)
                            .build();
                    rpcClient.call(request, responseConsumer);
                    return future;
                }).collect(Collectors.toList());

        CompletableFuture<List<DeviceCommand>> future = CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[futures.size()]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList()));
        return Pair.of(subscriptionId, future);
    }

    public void submitCommandUnsubscribe(String subId, Set<String> deviceGuids) {
        CommandUnsubscribeRequest unsubscribeRequest = new CommandUnsubscribeRequest(subId, deviceGuids);
        Request request = Request.newBuilder()
                .withBody(unsubscribeRequest)
                .build();
        rpcClient.push(request);
    }

    public CompletableFuture<Pair<String, DeviceCommand>> submitSubscribeOnUpdate(long commandId, String guid) {
        CompletableFuture<Response> future = new CompletableFuture<>();
        rpcClient.call(Request.newBuilder()
                .withBody(new CommandUpdateSubscribeRequest(commandId, guid))
                .build(), new ResponseConsumer(future));
        return future.thenApply(r -> {
            CommandUpdateSubscribeResponse response = r.getBody().cast(CommandUpdateSubscribeResponse.class);
            return Pair.of(response.getSubscriptionId(), response.getDeviceCommand());
        });
    }

    public CompletableFuture<Void> update(DeviceCommand cmd, DeviceCommandWrapper commandWrapper) {
        if (cmd == null) {
            throw new NoSuchElementException("Command not found");
        }
        cmd.setIsUpdated(true);

        if (commandWrapper.getCommand() != null) {
            cmd.setCommand(commandWrapper.getCommand().orElse(null));
        }
        if (commandWrapper.getParameters() != null) {
            cmd.setParameters(commandWrapper.getParameters().orElse(null));
        }
        if (commandWrapper.getLifetime() != null) {
            cmd.setLifetime(commandWrapper.getLifetime().orElse(null));
        }
        if (commandWrapper.getStatus() != null) {
            cmd.setStatus(commandWrapper.getStatus().orElse(null));
        }
        if (commandWrapper.getResult() != null) {
            cmd.setResult(commandWrapper.getResult().orElse(null));
        }

        hiveValidator.validate(cmd);

        CompletableFuture<Response> future = new CompletableFuture<>();
        rpcClient.call(Request.newBuilder()
                .withBody(new CommandUpdateRequest(cmd))
                .build(), new ResponseConsumer(future));
        return future.thenApply(response -> null);
    }

    private DeviceCommand convertWrapperToCommand(DeviceCommandWrapper commandWrapper, DeviceVO device, UserVO user) {
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
        return command;
    }
}
