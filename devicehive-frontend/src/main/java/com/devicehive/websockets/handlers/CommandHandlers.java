package com.devicehive.websockets.handlers;

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

import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.websockets.HiveWebsocketAuth;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.messages.handler.WebSocketClientHandler;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.SubscriptionInfo;
import com.devicehive.model.eventbus.Filter;
import com.devicehive.model.rpc.CommandSearchRequest;
import com.devicehive.model.updates.DeviceCommandUpdate;
import com.devicehive.model.wrappers.DeviceCommandWrapper;
import com.devicehive.resource.util.JsonTypes;
import com.devicehive.service.BaseFilterService;
import com.devicehive.service.DeviceCommandService;
import com.devicehive.service.DeviceService;
import com.devicehive.vo.*;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static com.devicehive.configuration.Constants.*;
import static com.devicehive.configuration.Messages.*;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;
import static com.devicehive.model.rpc.CommandSearchRequest.createCommandSearchRequest;
import static com.devicehive.shim.api.Action.COMMAND_EVENT;
import static com.devicehive.util.ServerResponsesFactory.createCommandMessage;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

@Component
public class CommandHandlers {

    private static final Logger logger = LoggerFactory.getLogger(CommandHandlers.class);

    public static final String SUBSCRIPTION_SET_NAME = "commandSubscriptions";

    private final Gson gson;
    private final DeviceService deviceService;
    private final DeviceCommandService commandService;
    private final BaseFilterService filterService;
    private final WebSocketClientHandler clientHandler;

    @Autowired
    public CommandHandlers(Gson gson,
                           DeviceService deviceService,
                           DeviceCommandService commandService,
                           BaseFilterService filterService,
                           WebSocketClientHandler clientHandler) {
        this.gson = gson;
        this.deviceService = deviceService;
        this.commandService = commandService;
        this.filterService = filterService;
        this.clientHandler = clientHandler;
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(#deviceId, 'GET_DEVICE_COMMAND')")
    @SuppressWarnings("unchecked")
    public void processCommandSubscribe(String deviceId, JsonObject request, WebSocketSession session)
            throws InterruptedException {
        final HiveAuthentication authentication = (HiveAuthentication) SecurityContextHolder.getContext().getAuthentication();
        final Date timestamp = gson.fromJson(request.get(TIMESTAMP), Date.class);
        final Set<String> names = gson.fromJson(request.getAsJsonArray(NAMES), JsonTypes.STRING_SET_TYPE);
        Set<Long> networks = gson.fromJson(request.getAsJsonArray(NETWORK_IDS), JsonTypes.LONG_SET_TYPE);
        Set<Long> deviceTypes = gson.fromJson(request.getAsJsonArray(DEVICE_TYPE_IDS), JsonTypes.LONG_SET_TYPE);
        final Integer limit = Optional.ofNullable(gson.fromJson(request.get(LIMIT), Integer.class)).orElse(DEFAULT_TAKE);
        final Boolean returnUpdated = Optional.ofNullable(gson.fromJson(request.get(RETURN_UPDATED_COMMANDS), Boolean.class))
                .orElse(DEFAULT_RETURN_UPDATED_COMMANDS);

        logger.debug("command/subscribe requested for device: {}. Networks: {}. Device types: {}. Timestamp: {}. Names {} Session: {}",
                deviceId, networks, deviceTypes, timestamp, names, session);

        Set<Filter> filters = filterService.getFilterList(deviceId, networks, deviceTypes, COMMAND_EVENT.name(), names, authentication);

        if (!filters.isEmpty()) {
            BiConsumer<DeviceCommand, Long> callback = (command, subscriptionId) -> {
                JsonObject json = createCommandMessage(command, subscriptionId, returnUpdated);
                clientHandler.sendMessage(json, session);
            };

            Pair<Long, CompletableFuture<List<DeviceCommand>>> pair = commandService
                    .sendSubscribeRequest(filters, names, timestamp, returnUpdated, limit, callback);

            logger.debug("command/subscribe done for devices: {}. Networks: {}. Device types: {}. Timestamp: {}. Names {} Session: {}",
                    deviceId, networks, deviceTypes, timestamp, names, session.getId());

            ((CopyOnWriteArraySet) session
                    .getAttributes()
                    .get(SUBSCRIPTION_SET_NAME))
                    .add(new SubscriptionInfo(pair.getLeft(), COMMAND, deviceId, networks, deviceTypes, names, timestamp));

            pair.getRight()
                    .thenAccept(collection -> {
                        WebSocketResponse response = new WebSocketResponse();
                        response.addValue(SUBSCRIPTION_ID, pair.getLeft(), null);
                        clientHandler.sendMessage(request, response, session);
                        collection.forEach(cmd -> clientHandler.sendMessage(createCommandMessage(cmd, pair.getLeft(), returnUpdated), session));
                    });
        } else {
            throw new HiveException(NO_ACCESS_TO_DEVICE_TYPES_OR_NETWORKS, SC_FORBIDDEN);
        }
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_DEVICE_COMMAND')")
    @SuppressWarnings("unchecked")
    public void processCommandUnsubscribe(JsonObject request, WebSocketSession session) {
        final Long subscriptionId = gson.fromJson(request.get(SUBSCRIPTION_ID), Long.class);
        Set<SubscriptionInfo> sessionSubscriptions = ((CopyOnWriteArraySet) session
                .getAttributes()
                .get(SUBSCRIPTION_SET_NAME));
        Set<Long> sessionSubIds = sessionSubscriptions.stream().map(SubscriptionInfo::getSubscriptionId).collect(Collectors.toSet());

        logger.debug("command/unsubscribe action. Session {} ", session.getId());
        if (subscriptionId != null && !sessionSubIds.contains(subscriptionId)) {
            throw new HiveException(String.format(Messages.SUBSCRIPTION_NOT_FOUND, subscriptionId), SC_NOT_FOUND);
        }
        
        CompletableFuture<Set<Long>> future;
        if (subscriptionId == null) {
            future = commandService.sendUnsubscribeRequest(sessionSubIds);
            sessionSubscriptions.clear();
        } else {
            future = commandService.sendUnsubscribeRequest(Collections.singleton(subscriptionId));
            sessionSubscriptions.remove(new SubscriptionInfo(subscriptionId));
        }
        
        future.thenAccept(collection -> {
            logger.debug("command/unsubscribe completed for session {}", session.getId());
            clientHandler.sendMessage(request, new WebSocketResponse(), session);    
        });
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(#deviceId, 'CREATE_DEVICE_COMMAND')")
    public void processCommandInsert(String deviceId, JsonObject request, WebSocketSession session) {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                
        final DeviceCommandWrapper deviceCommand = gson
                .fromJson(request.getAsJsonObject(COMMAND), DeviceCommandWrapper.class);

        logger.debug("command/insert action for {}, Session ", deviceId, session.getId());

        if (deviceId == null) {
            throw new HiveException(DEVICE_ID_REQUIRED, SC_BAD_REQUEST);
        }

        DeviceVO deviceVO = deviceService.findByIdWithPermissionsCheck(deviceId, principal);
        if (deviceVO == null) {
            throw new HiveException(String.format(DEVICE_NOT_FOUND, deviceId), SC_NOT_FOUND);
        }
        
        if (deviceCommand == null) {
            throw new HiveException(Messages.EMPTY_COMMAND, SC_BAD_REQUEST);
        }
        final UserVO user = principal.getUser();

        WebSocketResponse response = new WebSocketResponse();
        commandService.insert(deviceCommand, deviceVO, user)
                .thenAccept(command -> {
                    response.addValue(COMMAND, command, COMMAND_TO_CLIENT);
                    clientHandler.sendMessage(request, response, session);
                })
                .exceptionally(ex -> {
                    logger.warn("Unable to insert notification.", ex);
                    throw new HiveException(Messages.INTERNAL_SERVER_ERROR, SC_INTERNAL_SERVER_ERROR);
                });
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(#deviceId, 'UPDATE_DEVICE_COMMAND')")
    public void processCommandUpdate(String deviceId, JsonObject request, WebSocketSession session) {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final Long id = gson.fromJson(request.get(COMMAND_ID), Long.class);
        final DeviceCommandUpdate commandUpdate = gson
                .fromJson(request.getAsJsonObject(COMMAND), DeviceCommandUpdate.class);

        logger.debug("command/update requested for session: {}. Device ID: {}. Command id: {}", session, deviceId, id);
        if (id == null) {
            logger.debug("command/update canceled for session: {}. Command id is not provided", session);
            throw new HiveException(Messages.COMMAND_ID_REQUIRED, SC_BAD_REQUEST);
        }

        if (deviceId == null) {
            throw new HiveException(DEVICE_ID_REQUIRED, SC_BAD_REQUEST);
        }

        DeviceVO deviceVO = deviceService.findByIdWithPermissionsCheck(deviceId, principal);
        if (deviceVO == null) {
            throw new HiveException(String.format(DEVICE_NOT_FOUND, deviceId), SC_NOT_FOUND);
        }

        commandService.findOne(id, deviceVO.getDeviceId())
                .thenAccept(optionalCommand -> {
                    optionalCommand.map(deviceCommand -> commandService.update(deviceCommand, commandUpdate))
                            .orElseThrow(() -> new HiveException(String.format(COMMAND_NOT_FOUND, id), SC_NOT_FOUND));
                }).thenAccept(whenUpdated -> {
                    logger.debug("command/update proceed successfully for session: {}. Device ID: {}. Command id: {}",
                            session, deviceId, id);
                    clientHandler.sendMessage(request, new WebSocketResponse(), session);
                });
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(#deviceId, 'GET_DEVICE_COMMAND')")
    public void processCommandGet(String deviceId, JsonObject request, WebSocketSession session)  {
        if (deviceId == null) {
            logger.error("command/get proceed with error. Device ID should be provided.");
            throw new HiveException(DEVICE_ID_REQUIRED, SC_BAD_REQUEST);
        }
         
        Long commandId = gson.fromJson(request.get(COMMAND_ID), Long.class);
        if (commandId == null) {
            logger.error("command/get proceed with error. Command ID should be provided.");
            throw new HiveException(Messages.COMMAND_ID_REQUIRED, SC_BAD_REQUEST);
        }

        logger.debug("Device command get requested. deviceId = {}, commandId = {}", deviceId, commandId);
        DeviceVO device = deviceService.findById(deviceId);
        if (device == null) {
            logger.error("command/get proceed with error. No Device with Device ID = {} found.", deviceId);
            throw new HiveException(String.format(DEVICE_NOT_FOUND, deviceId), SC_NOT_FOUND);
        }
        
        WebSocketResponse webSocketResponse = commandService.findOne(commandId, deviceId)
                .thenApply(command -> command
                        .map(c -> {
                            logger.debug("Device command get proceed successfully deviceId = {} commandId = {}", deviceId, commandId);
                            WebSocketResponse response = new WebSocketResponse();
                            response.addValue(COMMAND, command.get(), COMMAND_TO_DEVICE);
                            return response;
                        }).orElse(null)
                ).exceptionally(ex -> {
                    logger.error("Unable to get command.", ex);
                    throw new HiveException(Messages.INTERNAL_SERVER_ERROR, SC_INTERNAL_SERVER_ERROR);
                }).join();
        
        if (webSocketResponse == null) {
            logger.error(String.format(COMMAND_NOT_FOUND, commandId));
            throw new HiveException(String.format(COMMAND_NOT_FOUND, commandId), SC_NOT_FOUND);
        }

        clientHandler.sendMessage(request, webSocketResponse, session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(#deviceId, 'GET_DEVICE_COMMAND')")
    public void processCommandList(String deviceId, JsonObject request, WebSocketSession session) {
        CommandSearchRequest commandSearchRequest = createCommandSearchRequest(request);
        
        if (deviceId == null) {
            logger.error("command/list proceed with error. Device ID should be provided.");
            throw new HiveException(DEVICE_ID_REQUIRED, SC_BAD_REQUEST);
        }
        
        logger.debug("Device command query requested for device {}", deviceId);

        DeviceVO device = deviceService.findById(deviceId);
        if (device == null) {
            logger.error("command/list proceed with error. No Device with Device ID = {} found.", deviceId);
            throw new HiveException(String.format(DEVICE_NOT_FOUND, deviceId), SC_NOT_FOUND);
        }
        
        WebSocketResponse response = new WebSocketResponse();
        
        commandService.find(commandSearchRequest)
                .thenAccept(sortedDeviceCommands -> {
                    response.addValue(COMMANDS, sortedDeviceCommands, COMMAND_LISTED);
                    clientHandler.sendMessage(request, response, session);
                })
                .exceptionally(ex -> {
                    logger.warn("Unable to get commands list.", ex);
                    throw new HiveException(Messages.INTERNAL_SERVER_ERROR, SC_INTERNAL_SERVER_ERROR);
                });
    }

}
