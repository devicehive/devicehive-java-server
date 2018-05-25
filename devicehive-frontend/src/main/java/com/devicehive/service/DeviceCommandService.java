package com.devicehive.service;

/*
 * #%L
 * DeviceHive Frontend Logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.api.RequestResponseMatcher;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.eventbus.Filter;
import com.devicehive.model.eventbus.events.CommandEvent;
import com.devicehive.model.eventbus.events.CommandUpdateEvent;
import com.devicehive.model.eventbus.events.CommandsUpdateEvent;
import com.devicehive.model.rpc.*;
import com.devicehive.model.updates.DeviceCommandUpdate;
import com.devicehive.model.wrappers.DeviceCommandWrapper;
import com.devicehive.service.helpers.LongIdGenerator;
import com.devicehive.service.helpers.ResponseConsumer;
import com.devicehive.service.time.TimestampService;
import com.devicehive.shim.api.Action;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.UserVO;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.devicehive.shim.api.Action.COMMAND_UPDATE_EVENT;

@Service
public class DeviceCommandService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceCommandService.class);

    private final TimestampService timestampService;
    private final HiveValidator hiveValidator;
    private final RpcClient rpcClient;
    private final LongIdGenerator idGenerator;
    private final RequestResponseMatcher requestResponseMatcher;

    @Autowired
    public DeviceCommandService(TimestampService timestampService,
                                HiveValidator hiveValidator,
                                RpcClient rpcClient,
                                LongIdGenerator idGenerator,
                                RequestResponseMatcher requestResponseMatcher) {
        this.timestampService = timestampService;
        this.hiveValidator = hiveValidator;
        this.rpcClient = rpcClient;
        this.idGenerator = idGenerator;
        this.requestResponseMatcher = requestResponseMatcher;
    }

    public CompletableFuture<Optional<DeviceCommand>> findOne(Long id, String deviceId, boolean returnUpdated) {
        CommandSearchRequest searchRequest = new CommandSearchRequest();
        searchRequest.setId(id);
        searchRequest.setDeviceIds(Collections.singleton(deviceId));
        searchRequest.setReturnUpdated(returnUpdated);

        CompletableFuture<Response> future = new CompletableFuture<>();
        rpcClient.call(Request.newBuilder()
                .withBody(searchRequest)
                .build(), new ResponseConsumer(future));
        return future.thenApply(r -> r.getBody().cast(CommandSearchResponse.class).getCommands().stream().findFirst());
    }

    public CompletableFuture<Optional<DeviceCommand>> findOne(Long id, String deviceId) {
        return findOne(id, deviceId, false);
    }

    public CompletableFuture<List<DeviceCommand>> find(CommandSearchRequest request) {
        
        return find(request.getDeviceIds(), request.getNames(), request.getTimestampStart(), request.getTimestampEnd(),
                request.getStatus(), request.getSortField(), request.getSortOrder(), request.getTake(), request.getSkip());
    }

    public CompletableFuture<List<DeviceCommand>> find(Collection<String> deviceIds, Collection<String> names,
            Date timestampSt, Date timestampEnd, String status, String sortField, String sortOrder, Integer take,
            Integer skip) {
        
        CommandSearchRequest searchRequest = new CommandSearchRequest();
        searchRequest.setDeviceIds(Sets.newHashSet(deviceIds));
        if (names != null) {
            searchRequest.setNames(Sets.newHashSet(names));
        }
        searchRequest.setTimestampStart(timestampSt);
        searchRequest.setTimestampEnd(timestampEnd);
        searchRequest.setStatus(status);
        searchRequest.setSortField(sortField);
        searchRequest.setSortOrder(sortOrder);
        searchRequest.setTake(take);
        searchRequest.setSkip(skip);

        CompletableFuture<Response> future = new CompletableFuture<>();
        rpcClient.call(Request.newBuilder()
                .withBody(searchRequest)
                .withPartitionKey(searchRequest.getDeviceId())
                .build(), new ResponseConsumer(future));
                    
        
        // CompletableFuture<Response> => CompletableFuture<List<DeviceCommand>>
        return future.thenApply(v -> v.getBody().cast(CommandSearchResponse.class).getCommands());
    }

    public CompletableFuture<DeviceCommand> insert(DeviceCommandWrapper commandWrapper, DeviceVO device, UserVO user) {
        hiveValidator.validate(commandWrapper);
        DeviceCommand command = convertWrapperToCommand(commandWrapper, device, user);

        CompletableFuture<Response> future = new CompletableFuture<>();
        rpcClient.call(Request.newBuilder()
                .withBody(new CommandInsertRequest(command))
                .withPartitionKey(device.getDeviceId())
                .build(), new ResponseConsumer(future));
        return future.thenApply(r -> ((CommandInsertResponse) r.getBody()).getDeviceCommand());
    }

    public Pair<Long, CompletableFuture<List<DeviceCommand>>> sendSubscribeRequest(
            final Set<Filter> filters,
            final Set<String> names,
            final Date timestamp,
            final boolean returnUpdated,
            final Integer limit,
            final BiConsumer<DeviceCommand, Long> callback) throws InterruptedException {

        final Long subscriptionId = idGenerator.generate();
        Collection<CompletableFuture<Collection<DeviceCommand>>> futures = filters.stream()
                .map(filter -> new CommandSubscribeRequest(subscriptionId, filter, names, timestamp, returnUpdated, limit))
                .map(subscribeRequest -> {
                    CompletableFuture<Collection<DeviceCommand>> future = new CompletableFuture<>();
                    Consumer<Response> responseConsumer = response -> {
                        Action resAction = response.getBody().getAction();
                        if (resAction.equals(Action.COMMAND_SUBSCRIBE_RESPONSE)) {
                            future.complete(response.getBody().cast(CommandSubscribeResponse.class).getCommands());
                            requestResponseMatcher.addSubscription(subscriptionId, response.getCorrelationId());
                        } else if (!returnUpdated && resAction.equals(Action.COMMAND_EVENT)) {
                            callback.accept(response.getBody().cast(CommandEvent.class).getCommand(), subscriptionId);
                        } else if (returnUpdated && resAction.equals(Action.COMMANDS_UPDATE_EVENT)) {
                            callback.accept(response.getBody().cast(CommandsUpdateEvent.class).getDeviceCommand(), subscriptionId);
                        } else {
                            logger.warn("Unknown action received from backend {}", resAction);
                        }
                    };
                    Request request = Request.newBuilder()
                            .withBody(subscribeRequest)
                            .withPartitionKey(subscribeRequest.getFilter().getFirstKey())
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

    public CompletableFuture<Set<Long>> sendUnsubscribeRequest(Set<Long> subIds) {
        CommandUnsubscribeRequest unsubscribeRequest = new CommandUnsubscribeRequest(subIds);
        Request request = Request.newBuilder()
                .withBody(unsubscribeRequest)
                .build();
        CompletableFuture<Set<Long>> future = new CompletableFuture<>();
        Consumer<Response> responseConsumer = response -> {
            Action resAction = response.getBody().getAction();
            if (resAction.equals(Action.COMMAND_UNSUBSCRIBE_RESPONSE)) {
                future.complete(response.getBody().cast(CommandUnsubscribeResponse.class).getSubscriptionIds());
                subIds.forEach(requestResponseMatcher::removeSubscription);
            } else {
                logger.warn("Unknown action received from backend {}", resAction);
            }
        };
        rpcClient.call(request, responseConsumer);
        return future;
    }

    public CompletableFuture<Pair<Long, DeviceCommand>> sendSubscribeToUpdateRequest(final long commandId, final DeviceVO device, BiConsumer<DeviceCommand, Long> callback) {
        CompletableFuture<Pair<Long, DeviceCommand>> future = new CompletableFuture<>();
        final Long subscriptionId = idGenerator.generate();
        Consumer<Response> responseConsumer = response -> {
            Action resAction = response.getBody().getAction();
            if (resAction.equals(Action.COMMAND_UPDATE_SUBSCRIBE_RESPONSE)) {
                future.complete(Pair.of(response.getBody().cast(CommandUpdateSubscribeResponse.class).getSubscriptionId(), response.getBody().cast(CommandUpdateSubscribeResponse.class).getDeviceCommand()));
            } else if (resAction.equals(COMMAND_UPDATE_EVENT)) {
                callback.accept(response.getBody().cast(CommandUpdateEvent.class).getDeviceCommand(), subscriptionId);
            } else {
                logger.warn("Unknown action received from backend {}", resAction);
            }
        };
        Filter filter = new Filter(device.getNetworkId(), device.getDeviceTypeId(), Long.toString(commandId), COMMAND_UPDATE_EVENT.name(), null);
        rpcClient.call(Request.newBuilder()
                .withBody(new CommandUpdateSubscribeRequest(commandId, device.getDeviceId(), subscriptionId, filter))
                .build(), responseConsumer);
        return future;
    }

    public CompletableFuture<Void> update(DeviceCommand cmd, DeviceCommandUpdate commandUpdate) {
        hiveValidator.validate(commandUpdate);
        if (cmd == null) {
            throw new NoSuchElementException("Command not found");
        }
        cmd.setIsUpdated(true);
        cmd.setLastUpdated(timestampService.getDate());

        if (commandUpdate.getStatus().isPresent()) {
            cmd.setStatus(commandUpdate.getStatus().get());
        }
        if (commandUpdate.getResult().isPresent()) {
            cmd.setResult(commandUpdate.getResult().get());
        }

        hiveValidator.validate(cmd);

        CompletableFuture<Response> commandUpdateFuture = new CompletableFuture<>();
        rpcClient.call(Request.newBuilder()
                .withBody(new CommandUpdateRequest(cmd))
                .build(), new ResponseConsumer(commandUpdateFuture));
        CompletableFuture<Response> commandsUpdateFuture = new CompletableFuture<>();
        rpcClient.call(Request.newBuilder()
                .withBody(new CommandsUpdateRequest(cmd))
                .build(), new ResponseConsumer(commandsUpdateFuture));
        return CompletableFuture.allOf(commandUpdateFuture, commandsUpdateFuture).thenApply(response -> null);
    }

    private DeviceCommand convertWrapperToCommand(DeviceCommandWrapper commandWrapper, DeviceVO device, UserVO user) {
        DeviceCommand command = new DeviceCommand();
        command.setId(Math.abs(new Random().nextInt()));
        command.setDeviceId(device.getDeviceId());
        command.setNetworkId(device.getNetworkId());
        command.setDeviceTypeId(device.getDeviceTypeId());
        command.setIsUpdated(false);

        if (commandWrapper.getTimestamp().isPresent()) {
            command.setTimestamp(commandWrapper.getTimestamp().get());
        } else {
            command.setTimestamp(timestampService.getDate());
        }
        
        command.setLastUpdated(command.getTimestamp());

        if (user != null) {
            command.setUserId(user.getId());
        }
        if (commandWrapper.getCommand().isPresent()) {
            command.setCommand(commandWrapper.getCommand().get());
        }
        if (commandWrapper.getParameters().isPresent()) {
            command.setParameters(commandWrapper.getParameters().get());
        }
        if (commandWrapper.getLifetime().isPresent()) {
            command.setLifetime(commandWrapper.getLifetime().get());
        }
        if (commandWrapper.getStatus().isPresent()) {
            command.setStatus(commandWrapper.getStatus().get());
        }
        if (commandWrapper.getResult().isPresent()) {
            command.setResult(commandWrapper.getResult().get());
        }

        hiveValidator.validate(command);
        return command;
    }
}
