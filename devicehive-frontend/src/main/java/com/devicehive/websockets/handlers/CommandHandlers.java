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
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.websockets.InsertCommand;
import com.devicehive.model.wrappers.DeviceCommandWrapper;
import com.devicehive.resource.util.JsonTypes;
import com.devicehive.service.DeviceCommandService;
import com.devicehive.service.DeviceService;
import com.devicehive.util.ServerResponsesFactory;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.UserVO;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
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
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.COMMAND_TO_CLIENT;
import static com.devicehive.messages.handler.WebSocketClientHandler.sendMessage;
import static javax.servlet.http.HttpServletResponse.*;

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

    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_DEVICE_COMMAND')")
    public WebSocketResponse processCommandSubscribe(JsonObject request, WebSocketSession session)
            throws InterruptedException {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final Date timestamp = gson.fromJson(request.get(TIMESTAMP), Date.class);
        final String deviceId = Optional.ofNullable(request.get(Constants.DEVICE_ID))
                .map(JsonElement::getAsString)
                .orElse(null);
        final Set<String> names = gson.fromJson(request.getAsJsonArray(NAMES), JsonTypes.STRING_SET_TYPE);
        Set<String> devices = gson.fromJson(request.getAsJsonArray(DEVICE_IDS), JsonTypes.STRING_SET_TYPE);
        final Integer limit = Optional.ofNullable(request.get(LIMIT)).map(JsonElement::getAsInt).orElse(DEFAULT_TAKE);

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
            actualDevices = deviceService.list(null, null, null, null, null, true, null, null, principal).join();
            devices = actualDevices.stream().map(DeviceVO::getDeviceId).collect(Collectors.toSet());
        }

        BiConsumer<DeviceCommand, String> callback = (command, subscriptionId) -> {
            JsonObject json = ServerResponsesFactory.createCommandInsertMessage(command, subscriptionId);
            sendMessage(json, session);
        };

        Pair<String, CompletableFuture<List<DeviceCommand>>> pair = commandService
                .sendSubscribeRequest(devices, names, timestamp, limit, callback);

        pair.getRight().thenAccept(collection ->
                collection.forEach(cmd ->
                        sendMessage(ServerResponsesFactory.createCommandInsertMessage(cmd, pair.getLeft()), session)));

        logger.debug("command/subscribe done for devices: {}, {}. Timestamp: {}. Names {} Session: {}",
                devices, deviceId, timestamp, names, session.getId());

        ((CopyOnWriteArraySet) session
                .getAttributes()
                .get(SUBSCSRIPTION_SET_NAME))
                .add(pair.getLeft());

        WebSocketResponse response = new WebSocketResponse();
        response.addValue(SUBSCRIPTION_ID, pair.getLeft(), null);
        return response;
    }

    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_DEVICE_COMMAND')")
    public WebSocketResponse processCommandUnsubscribe(JsonObject request, WebSocketSession session) {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final Optional<String> subscriptionId = Optional.ofNullable(request.get(SUBSCRIPTION_ID))
                .map(JsonElement::getAsString);
        Set<String> deviceIds = gson.fromJson(request.getAsJsonArray(DEVICE_IDS), JsonTypes.STRING_SET_TYPE);

        logger.debug("command/unsubscribe action. Session {} ", session.getId());
        if (!subscriptionId.isPresent() && deviceIds == null) {
            List<DeviceVO> actualDevices = deviceService.list(null, null, null, null, null, true, null, null, principal).join();
            deviceIds = actualDevices.stream().map(DeviceVO::getDeviceId).collect(Collectors.toSet());
            commandService.sendUnsubscribeRequest(null, deviceIds);
        } else if (subscriptionId.isPresent()) {
            commandService.sendUnsubscribeRequest(subscriptionId.get(), deviceIds);
        } else {
            commandService.sendUnsubscribeRequest(null, deviceIds);
        }

        ((CopyOnWriteArraySet) session
                .getAttributes()
                .get(SUBSCSRIPTION_SET_NAME))
                .remove(subscriptionId);

        return new WebSocketResponse();
    }

    @PreAuthorize("isAuthenticated() and hasPermission(null, 'CREATE_DEVICE_COMMAND')")
    public WebSocketResponse processCommandInsert(JsonObject request, WebSocketSession session) {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final String deviceId = Optional.ofNullable(request.get(Constants.DEVICE_ID))
                .map(JsonElement::getAsString)
                .orElse(null);
        final DeviceCommandWrapper deviceCommand = gson
                .fromJson(request.getAsJsonObject(COMMAND), DeviceCommandWrapper.class);

        logger.debug("command/insert action for {}, Session ", deviceId, session.getId());

        Set<DeviceVO> devices = new HashSet<>();
        if (deviceId == null) {
            devices.addAll(principal.getDeviceIds().stream()
                    .map(id -> deviceService.findByIdWithPermissionsCheck(id, principal))
                    .collect(Collectors.toList()));
        } else {
           devices.add(deviceService.findByIdWithPermissionsCheck(deviceId, principal));
        }

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
                    .thenApply(cmd -> {
                        commandUpdateSubscribeAction(cmd.getId(), device.getDeviceId(), session);
                        response.addValue(COMMAND, new InsertCommand(cmd.getId(), cmd.getTimestamp(), cmd.getUserId()), COMMAND_TO_CLIENT);
                        return response;
                    })
                    .exceptionally(ex -> {
                        logger.warn("Unable to insert notification.", ex);
                        throw new HiveException(Messages.INTERNAL_SERVER_ERROR, SC_INTERNAL_SERVER_ERROR);
                    }).join();
        }

        return response;
    }

    @PreAuthorize("isAuthenticated() and hasPermission(null, 'UPDATE_DEVICE_COMMAND')")
    public WebSocketResponse processCommandUpdate(JsonObject request, WebSocketSession session) {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String deviceId = request.get(DEVICE_ID).getAsString();
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
        return new WebSocketResponse();
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

    private void commandUpdateSubscribeAction(Long commandId, String deviceId, WebSocketSession session) {
        if (commandId == null) {
            throw new HiveException(String.format(Messages.COLUMN_CANNOT_BE_NULL, "commandId"), SC_BAD_REQUEST);
        }
        BiConsumer<DeviceCommand, String> callback =  (command, subscriptionId) -> {
            JsonObject json = ServerResponsesFactory.createCommandUpdateMessage(command);
            sendMessage(json, session);
        };
        commandService.sendSubscribeToUpdateRequest(commandId, deviceId, callback); // TODO: make sure this is the correct place to create update message
    }
}
