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
import com.devicehive.auth.websockets.HiveWebsocketAuth;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.messages.handler.WebSocketClientHandler;
import com.devicehive.model.rpc.ListDeviceRequest;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.service.DeviceService;
import com.devicehive.vo.DeviceVO;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.Optional;

import static com.devicehive.configuration.Constants.*;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICES_LISTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_PUBLISHED;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

@Component
public class DeviceHandlers {

    private static final Logger logger = LoggerFactory.getLogger(DeviceHandlers.class);

    private final DeviceService deviceService;
    private final WebSocketClientHandler webSocketClientHandler;
    private final Gson gson;

    @Autowired
    public DeviceHandlers(DeviceService deviceService,
                          WebSocketClientHandler webSocketClientHandler,
                          Gson gson) {
        this.deviceService = deviceService;
        this.webSocketClientHandler = webSocketClientHandler;
        this.gson = gson;
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(#deviceId, 'REGISTER_DEVICE')")
    public void processDeviceDelete(String deviceId, JsonObject request, WebSocketSession session) throws HiveException {
        if (deviceId == null) {
            logger.error("device/delete proceed with error. Device ID should be provided.");
            throw new HiveException(Messages.DEVICE_ID_REQUIRED, SC_BAD_REQUEST);
        }
        
        boolean isDeviceDeleted = deviceService.deleteDevice(deviceId);
        if (!isDeviceDeleted) {
            logger.error("device/delete proceed with error. No Device with Device ID = {} found.", deviceId);
            throw new HiveException(String.format(Messages.DEVICE_NOT_FOUND, deviceId), SC_NOT_FOUND);
        }
        
        logger.debug("Device with id = {} is deleted", deviceId);
        webSocketClientHandler.sendMessage(request, new WebSocketResponse(), session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(#deviceId, 'GET_DEVICE')")
    public void processDeviceGet(String deviceId, JsonObject request, WebSocketSession session) throws HiveException {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        WebSocketResponse response = new WebSocketResponse();

        if (deviceId == null) {
            logger.error("device/get proceed with error. Device ID should be provided.");
            throw new HiveException(Messages.DEVICE_ID_REQUIRED, SC_BAD_REQUEST);
        }

        DeviceVO toResponse = deviceService.findByIdWithPermissionsCheck(deviceId, principal);

        if (toResponse == null) {
            logger.error("device/get proceed with error. No Device with Device ID = {} found.", deviceId);
            throw new HiveException(String.format(Messages.DEVICE_NOT_FOUND, deviceId), SC_NOT_FOUND);
        }
        
        response.addValue(Constants.DEVICE, toResponse, DEVICE_PUBLISHED);
        webSocketClientHandler.sendMessage(request, response, session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_DEVICE')")
    public void processDeviceList(JsonObject request, WebSocketSession session) throws HiveException {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        ListDeviceRequest listDeviceRequest = ListDeviceRequest.createListDeviceRequest(request, principal);

        String sortField = Optional.ofNullable(listDeviceRequest.getSortField()).map(String::toLowerCase).orElse(null);
        if (sortField != null
                && !NAME.equalsIgnoreCase(sortField)
                && !STATUS.equalsIgnoreCase(sortField)
                && !NETWORK.equalsIgnoreCase(sortField)) {
            logger.error("Unable to proceed device list request. Invalid sortField.");
            throw new HiveException(Messages.INVALID_REQUEST_PARAMETERS, BAD_REQUEST.getStatusCode());
        }

        WebSocketResponse response = new WebSocketResponse();
        if (!principal.areAllNetworksAvailable() && (principal.getNetworkIds() == null || principal.getNetworkIds().isEmpty()) &&
                !principal.areAllDevicesAvailable() && (principal.getDeviceIds() == null || principal.getDeviceIds().isEmpty())) {
            logger.warn("Unable to get list for empty devices");
            response.addValue(DEVICES, Collections.<DeviceVO>emptyList(), DEVICES_LISTED);
            webSocketClientHandler.sendMessage(request, response, session);
        } else {
            deviceService.list(listDeviceRequest)
                    .thenAccept(devices -> {
                        logger.debug("Device list request proceed successfully");
                        response.addValue(DEVICES, devices, DEVICES_LISTED);
                        webSocketClientHandler.sendMessage(request, response, session);
                    });
        }
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'REGISTER_DEVICE')")
    public void processDeviceSave(String deviceId, JsonObject request, WebSocketSession session) throws HiveException {
        DeviceUpdate device = gson.fromJson(request.get(Constants.DEVICE), DeviceUpdate.class);

        logger.debug("device/save process started for session {}", session.getId());
        if (deviceId == null) {
            throw new HiveException(Messages.DEVICE_ID_REQUIRED, SC_BAD_REQUEST);
        }
        if (!deviceId.matches("[a-zA-Z0-9-]+")) {
            throw new HiveException(Messages.DEVICE_ID_CONTAINS_INVALID_CHARACTERS, SC_BAD_REQUEST);
        }
        HivePrincipal hivePrincipal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        deviceService.deviceSaveAndNotify(deviceId, device, hivePrincipal).thenAccept(actionName -> {
            logger.debug("device/save process ended for session  {}", session.getId());
            webSocketClientHandler.sendMessage(request, new WebSocketResponse(), session);
        });
        
    }
}
