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

import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.messages.handler.WebSocketClientHandler;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.rpc.ListCommandRequest;
import com.devicehive.model.rpc.ListDeviceRequest;
import com.devicehive.model.wrappers.DeviceCommandWrapper;
import com.devicehive.resource.util.CommandResponseFilterAndSort;
import com.devicehive.resource.util.JsonTypes;
import com.devicehive.service.DeviceCommandService;
import com.devicehive.service.DeviceService;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.UserVO;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static com.devicehive.configuration.Constants.COMMAND;
import static com.devicehive.configuration.Constants.COMMANDS;
import static com.devicehive.configuration.Constants.COMMAND_ID;
import static com.devicehive.configuration.Constants.DEFAULT_RETURN_UPDATED_COMMANDS;
import static com.devicehive.configuration.Constants.DEFAULT_TAKE;
import static com.devicehive.configuration.Constants.DEVICE_ID;
import static com.devicehive.configuration.Constants.DEVICE_IDS;
import static com.devicehive.configuration.Constants.LIMIT;
import static com.devicehive.configuration.Constants.NAMES;
import static com.devicehive.configuration.Constants.RETURN_UPDATED_COMMANDS;
import static com.devicehive.configuration.Constants.SUBSCRIPTION_ID;
import static com.devicehive.configuration.Constants.TIMESTAMP;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;
import static com.devicehive.model.enums.SortOrder.ASC;
import static com.devicehive.model.rpc.ListCommandRequest.createListCommandRequest;
import static com.devicehive.util.ServerResponsesFactory.createCommandMessage;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

@Component
public class CommandHandlers {

    private static final Logger logger = LoggerFactory.getLogger(CommandHandlers.class);

    public static final String SUBSCSRIPTION_SET_NAME = "commandSubscriptions";

    @Autowired
    private Gson gson;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceCommandService commandService;

    @Autowired
    private WebSocketClientHandler clientHandler;

    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_DEVICE_COMMAND')")
    public void processCommandSubscribe(JsonObject request, WebSocketSession session)
            throws InterruptedException {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final Date timestamp = gson.fromJson(request.get(TIMESTAMP), Date.class);
        final String deviceId = gson.fromJson(request.get(DEVICE_ID), String.class);
        final Set<String> names = gson.fromJson(request.getAsJsonArray(NAMES), JsonTypes.STRING_SET_TYPE);
        Set<String> devices = gson.fromJson(request.getAsJsonArray(DEVICE_IDS), JsonTypes.STRING_SET_TYPE);
        final Integer limit = Optional.ofNullable(gson.fromJson(request.get(LIMIT), Integer.class)).orElse(DEFAULT_TAKE);
        final Boolean returnUpdated = Optional.ofNullable(gson.fromJson(request.get(RETURN_UPDATED_COMMANDS), Boolean.class))
                .orElse(DEFAULT_RETURN_UPDATED_COMMANDS);

        logger.debug("command/subscribe requested for devices: {}, {}. Timestamp: {}. Names {} Session: {}",
                devices, deviceId, timestamp, names, session);

        devices = prepareActualList(devices, deviceId);

        List<DeviceVO> actualDevices;
        if (devices != null) {
            actualDevices = deviceService.findByIdWithPermissionsCheck(devices, principal);
            if (actualDevices.size() != devices.size()) {
                throw new HiveException(String.format(Messages.DEVICES_NOT_FOUND, devices), SC_FORBIDDEN);
            }
        } else {
            ListDeviceRequest listDeviceRequest = new ListDeviceRequest(ASC.name(), principal);
            actualDevices = deviceService.list(listDeviceRequest).join();
            devices = actualDevices.stream().map(DeviceVO::getDeviceId).collect(Collectors.toSet());
        }

        BiConsumer<DeviceCommand, String> callback = (command, subscriptionId) -> {
            JsonObject json = createCommandMessage(command, subscriptionId, returnUpdated);
            clientHandler.sendMessage(json, session);
        };

        Pair<String, CompletableFuture<List<DeviceCommand>>> pair = commandService
                .sendSubscribeRequest(devices, names, timestamp, returnUpdated, limit, callback);

        pair.getRight().thenAccept(collection -> 
                collection.forEach(cmd -> clientHandler.sendMessage(createCommandMessage(cmd, pair.getLeft(), returnUpdated), session)));

        logger.debug("command/subscribe done for devices: {}, {}. Timestamp: {}. Names {} Session: {}",
                devices, deviceId, timestamp, names, session.getId());

        ((CopyOnWriteArraySet) session
                .getAttributes()
                .get(SUBSCSRIPTION_SET_NAME))
                .add(pair.getLeft());

        WebSocketResponse response = new WebSocketResponse();
        response.addValue(SUBSCRIPTION_ID, pair.getLeft(), null);
        clientHandler.sendMessage(request, response, session);
    }

    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_DEVICE_COMMAND')")
    public void processCommandUnsubscribe(JsonObject request, WebSocketSession session) {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final String subscriptionId = gson.fromJson(request.get(SUBSCRIPTION_ID), String.class);
        Set<String> deviceIds = gson.fromJson(request.getAsJsonArray(DEVICE_IDS), JsonTypes.STRING_SET_TYPE);
        CopyOnWriteArraySet sessionSubIds = ((CopyOnWriteArraySet) session
                .getAttributes()
                .get(SUBSCSRIPTION_SET_NAME));

        logger.debug("command/unsubscribe action. Session {} ", session.getId());
        if (subscriptionId != null && !sessionSubIds.contains(subscriptionId)) {
            throw new HiveException(String.format(Messages.SUBSCRIPTION_NOT_FOUND, subscriptionId), SC_NOT_FOUND);
        }
        if (subscriptionId == null && deviceIds == null) {
            ListDeviceRequest listDeviceRequest = new ListDeviceRequest(ASC.name(), principal);
            List<DeviceVO> actualDevices = deviceService.list(listDeviceRequest).join();
            deviceIds = actualDevices.stream().map(DeviceVO::getDeviceId).collect(Collectors.toSet());
            commandService.sendUnsubscribeRequest(null, deviceIds);
        } else {
            commandService.sendUnsubscribeRequest(subscriptionId, deviceIds);
        }

        sessionSubIds.remove(subscriptionId);

        clientHandler.sendMessage(request, new WebSocketResponse(), session);
    }

    @PreAuthorize("isAuthenticated() and hasPermission(null, 'CREATE_DEVICE_COMMAND')")
    public void processCommandInsert(JsonObject request, WebSocketSession session) {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final String deviceId = gson.fromJson(request.get(DEVICE_ID), String.class);
                
        final DeviceCommandWrapper deviceCommand = gson
                .fromJson(request.getAsJsonObject(COMMAND), DeviceCommandWrapper.class);

        logger.debug("command/insert action for {}, Session ", deviceId, session.getId());

        Set<DeviceVO> devices = new HashSet<>();
        if (deviceId == null) {
            throw new HiveException(Messages.DEVICE_ID_REQUIRED, SC_BAD_REQUEST);
        }
        
        devices.add(deviceService.findByIdWithPermissionsCheck(deviceId, principal));
        
        if (devices.isEmpty()) {
            throw new HiveException(String.format(Messages.DEVICE_NOT_FOUND, deviceId), SC_NOT_FOUND);
        }
        if (deviceCommand == null) {
            throw new HiveException(Messages.EMPTY_COMMAND, SC_BAD_REQUEST);
        }
        final UserVO user = principal.getUser();

        WebSocketResponse response = new WebSocketResponse();
        for (DeviceVO device : devices) {
            commandService.insert(deviceCommand, device, user)
                    .thenAccept(command -> {
                        response.addValue(COMMAND, command, COMMAND_TO_CLIENT);
                        clientHandler.sendMessage(request, response, session);
                    })
                    .exceptionally(ex -> {
                        logger.warn("Unable to insert notification.", ex);
                        throw new HiveException(Messages.INTERNAL_SERVER_ERROR, SC_INTERNAL_SERVER_ERROR);
                    });
        }
    }

    @PreAuthorize("isAuthenticated() and hasPermission(null, 'UPDATE_DEVICE_COMMAND')")
    public void processCommandUpdate(JsonObject request, WebSocketSession session) {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String deviceId = gson.fromJson(request.get(DEVICE_ID), String.class);;
        final Long id = Long.valueOf(request.get(COMMAND_ID).getAsString()); // TODO: nullable long?
        final DeviceCommandWrapper commandUpdate = gson
                .fromJson(request.getAsJsonObject(COMMAND), DeviceCommandWrapper.class);

        logger.debug("command/update requested for session: {}. Device ID: {}. Command id: {}", session, deviceId, id);
        if (id == null) {
            logger.debug("command/update canceled for session: {}. Command id is not provided", session);
            throw new HiveException(Messages.COMMAND_ID_REQUIRED, SC_BAD_REQUEST);
        }

        Set<DeviceVO> devices = new HashSet<>();
        if (deviceId == null) {
            devices.addAll(principal.getDeviceIds().stream()
                    .map(devId -> deviceService.findByIdWithPermissionsCheck(devId, principal))
                    .collect(Collectors.toList()));
        } else {
            devices.add(deviceService.findByIdWithPermissionsCheck(deviceId, principal));
        }

        if (devices.isEmpty()) {
            throw new HiveException(String.format(Messages.DEVICE_NOT_FOUND, id), SC_NOT_FOUND);
        }

        Optional<DeviceCommand> savedCommand = Optional.empty();
        for (DeviceVO device : devices) {
            savedCommand = commandService.findOne(id, device.getDeviceId()).join();
            if (savedCommand.isPresent()) {
                commandService.update(savedCommand.get(), commandUpdate);
            }
        }

        if (!savedCommand.isPresent()) {
            throw new HiveException(String.format(Messages.COMMAND_NOT_FOUND, id), SC_NOT_FOUND);
        }

        logger.debug("command/update proceed successfully for session: {}. Device ID: {}. Command id: {}", session,
                deviceId, id);
        clientHandler.sendMessage(request, new WebSocketResponse(), session);
    }

    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_DEVICE_COMMAND')")
    public void processCommandGet(JsonObject request, WebSocketSession session)  {
        String deviceId = gson.fromJson(request.get(DEVICE_ID), String.class);
        if (deviceId == null) {
            logger.error("command/get proceed with error. Device ID should be provided.");
            throw new HiveException(Messages.DEVICE_ID_REQUIRED, SC_BAD_REQUEST);
        }
         
        Long commandId = gson.fromJson(request.get(COMMAND_ID), Long.class);
        if (commandId == null) {
            logger.error("command/get proceed with error. Device ID should be provided.");
            throw new HiveException(Messages.COMMAND_ID_REQUIRED, SC_BAD_REQUEST);
        }

        logger.debug("Device command get requested. deviceId = {}, commandId = {}", deviceId, commandId);
        DeviceVO device = deviceService.findById(deviceId);
        if (device == null) {
            logger.error("command/get proceed with error. No Device with Device ID = {} found.", deviceId);
            throw new HiveException(String.format(Messages.DEVICE_NOT_FOUND, deviceId), SC_NOT_FOUND);
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
            logger.error(String.format(Messages.COMMAND_NOT_FOUND, commandId));
            throw new HiveException(String.format(Messages.COMMAND_NOT_FOUND, commandId), SC_NOT_FOUND);
        }

        clientHandler.sendMessage(request, webSocketResponse, session);
    }

    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_DEVICE_COMMAND')")
    public void processCommandList(JsonObject request, WebSocketSession session) {
        ListCommandRequest listCommandRequest = createListCommandRequest(request);
        String deviceId = listCommandRequest.getDeviceId();
        if (deviceId == null) {
            logger.error("command/list proceed with error. Device ID should be provided.");
            throw new HiveException(Messages.DEVICE_ID_REQUIRED, SC_BAD_REQUEST);
        }
        
        logger.debug("Device command query requested for device {}", deviceId);

        DeviceVO device = deviceService.findById(deviceId);
        if (device == null) {
            logger.error("command/list proceed with error. No Device with Device ID = {} found.", deviceId);
            throw new HiveException(String.format(Messages.DEVICE_NOT_FOUND, deviceId), SC_NOT_FOUND);
        }
        
        WebSocketResponse response = new WebSocketResponse();
        
        commandService.find(listCommandRequest)
                .thenAccept(commands -> {
                    final Comparator<DeviceCommand> comparator = CommandResponseFilterAndSort
                            .buildDeviceCommandComparator(listCommandRequest.getSortField());
                    final String sortOrderSt = listCommandRequest.getSortOrder();  
                    final Boolean reverse = sortOrderSt == null ? null : "desc".equalsIgnoreCase(sortOrderSt);
                    
                    final List<DeviceCommand> sortedDeviceCommands = CommandResponseFilterAndSort
                            .orderAndLimit(new ArrayList<>(commands), comparator, reverse,
                                    listCommandRequest.getSkip(), listCommandRequest.getTake());
                    response.addValue(COMMANDS, sortedDeviceCommands, COMMAND_LISTED);
                    clientHandler.sendMessage(request, response, session);
                })
                .exceptionally(ex -> {
                    logger.warn("Unable to get commands list.", ex);
                    throw new HiveException(Messages.INTERNAL_SERVER_ERROR, SC_INTERNAL_SERVER_ERROR);
                });
    }

    private Set<String> prepareActualList(Set<String> deviceIdSet, final String deviceId) {
        if (deviceId == null && deviceIdSet == null) {
            return null;
        }
        if (deviceIdSet != null && deviceId == null) {
            deviceIdSet.remove(null);
            return deviceIdSet;
        }
        if (deviceIdSet == null) {
            return new HashSet<String>() {
                {
                    add(deviceId);
                }

                private static final long serialVersionUID = -8657632518613033661L;
            };
        }
        throw new HiveException(Messages.INVALID_REQUEST_PARAMETERS, SC_BAD_REQUEST);
    }

}
