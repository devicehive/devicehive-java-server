package com.devicehive.websockets.handlers;

/*
 * #%L
 * DeviceHive Frontend Logic
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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
import com.devicehive.model.rpc.CountDeviceTypeRequest;
import com.devicehive.model.rpc.ListDeviceTypeRequest;
import com.devicehive.model.updates.DeviceTypeUpdate;
import com.devicehive.service.BaseDeviceTypeService;
import com.devicehive.service.DeviceTypeService;
import com.devicehive.vo.DeviceTypeVO;
import com.devicehive.vo.DeviceTypeWithUsersAndDevicesVO;
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
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Component
public class DeviceTypeHandlers {
    private static final Logger logger = LoggerFactory.getLogger(DeviceTypeHandlers.class);

    private final DeviceTypeService deviceTypeService;
    private final WebSocketClientHandler webSocketClientHandler;
    private final Gson gson;

    @Autowired
    public DeviceTypeHandlers(DeviceTypeService deviceTypeService, WebSocketClientHandler webSocketClientHandler, Gson gson) {
        this.deviceTypeService = deviceTypeService;
        this.webSocketClientHandler = webSocketClientHandler;
        this.gson = gson;
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_DEVICE_TYPE')")
    public void processDeviceTypeList(JsonObject request, WebSocketSession session) {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        ListDeviceTypeRequest listDeviceTypeRequest = ListDeviceTypeRequest.createListDeviceTypeRequest(request);
        listDeviceTypeRequest.setPrincipal(Optional.ofNullable(principal));

        String sortField = Optional.ofNullable(listDeviceTypeRequest.getSortField()).map(String::toLowerCase).orElse(null);
        if (sortField != null && !ID.equalsIgnoreCase(sortField) && !NAME.equalsIgnoreCase(sortField)) {
            logger.error("Unable to proceed device type list request. Invalid sortField");
            throw new HiveException(Messages.INVALID_REQUEST_PARAMETERS, BAD_REQUEST.getStatusCode());
        }

        WebSocketResponse response = new WebSocketResponse();
        if (!principal.areAllDeviceTypesAvailable() && (principal.getDeviceTypeIds() == null || principal.getDeviceTypeIds().isEmpty())) {
            logger.warn("Unable to get list for empty device types");
            response.addValue(DEVICE_TYPES, Collections.<DeviceTypeVO>emptyList(), DEVICE_TYPES_LISTED);
            webSocketClientHandler.sendMessage(request, response, session);
        } else {
            deviceTypeService.list(listDeviceTypeRequest)
                    .thenAccept(deviceTypes -> {
                        logger.debug("Device type list request proceed successfully.");
                        response.addValue(DEVICE_TYPES, deviceTypes, DEVICE_TYPES_LISTED);
                        webSocketClientHandler.sendMessage(request, response, session);
                    });
        }
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_DEVICE_TYPE')")
    public void processDeviceTypeCount(JsonObject request, WebSocketSession session) {
        CountDeviceTypeRequest countDeviceTypeRequest = CountDeviceTypeRequest.createCountDeviceTypeRequest(request);

        WebSocketResponse response = new WebSocketResponse();
        deviceTypeService.count(countDeviceTypeRequest)
                .thenAccept(count -> {
                    logger.debug("Device type count request proceed successfully.");
                    response.addValue(COUNT, count.getCount(), null);
                    webSocketClientHandler.sendMessage(request, response, session);
                });
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(#deviceTypeId, 'GET_DEVICE_TYPE')")
    public void processDeviceTypeGet(Long deviceTypeId, JsonObject request, WebSocketSession session) {
        logger.debug("Device type get requested.");
        if (deviceTypeId == null) {
            logger.error(Messages.DEVICE_TYPE_ID_REQUIRED);
            throw new HiveException(Messages.DEVICE_TYPE_ID_REQUIRED, BAD_REQUEST.getStatusCode());
        }

        DeviceTypeWithUsersAndDevicesVO existing = deviceTypeService.getWithDevices(deviceTypeId);
        if (existing == null) {
            logger.error(String.format(Messages.DEVICE_TYPE_NOT_FOUND, deviceTypeId));
            throw new HiveException(String.format(Messages.DEVICE_TYPE_NOT_FOUND, deviceTypeId), NOT_FOUND.getStatusCode());
        }

        WebSocketResponse response = new WebSocketResponse();
        response.addValue(DEVICE_TYPE, existing, DEVICE_TYPE_PUBLISHED);
        webSocketClientHandler.sendMessage(request, response, session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_DEVICE_TYPE')")
    public void processDeviceTypeInsert(JsonObject request, WebSocketSession session) {
        logger.debug("Device type insert requested");
        DeviceTypeVO deviceType = gson.fromJson(request.get(DEVICE_TYPE), DeviceTypeVO.class);
        if (deviceType == null) {
            logger.error(Messages.DEVICE_TYPE_REQUIRED);
            throw new HiveException(Messages.DEVICE_TYPE_REQUIRED, BAD_REQUEST.getStatusCode());
        }
        DeviceTypeVO result = deviceTypeService.create(deviceType);
        logger.debug("New device type has been created");

        WebSocketResponse response = new WebSocketResponse();
        response.addValue(DEVICE_TYPE, result, DEVICE_TYPE_SUBMITTED);
        webSocketClientHandler.sendMessage(request, response, session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(#deviceTypeId, 'MANAGE_DEVICE_TYPE')")
    public void processDeviceTypeUpdate(Long deviceTypeId, JsonObject request, WebSocketSession session) {
        DeviceTypeUpdate deviceTypeToUpdate = gson.fromJson(request.get(DEVICE_TYPE), DeviceTypeUpdate.class);
        logger.debug("Device type update requested. Id : {}", deviceTypeId);
        if (deviceTypeId == null) {
            logger.error(Messages.DEVICE_TYPE_ID_REQUIRED);
            throw new HiveException(Messages.DEVICE_TYPE_ID_REQUIRED, BAD_REQUEST.getStatusCode());
        }
        deviceTypeService.update(deviceTypeId, deviceTypeToUpdate);
        logger.debug("Device type has been updated successfully. Id : {}", deviceTypeId);
        webSocketClientHandler.sendMessage(request, new WebSocketResponse(), session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(#deviceTypeId, 'MANAGE_DEVICE_TYPE')")
    public void processDeviceTypeDelete(Long deviceTypeId, JsonObject request, WebSocketSession session) {
        logger.debug("Device type delete requested");
        boolean force = Optional.ofNullable(gson.fromJson(request.get(FORCE), Boolean.class)).orElse(false);
        boolean isDeleted = deviceTypeService.delete(deviceTypeId, force);
        if (!isDeleted) {
            logger.error(String.format(Messages.DEVICE_TYPE_NOT_FOUND, deviceTypeId));
            throw new HiveException(String.format(Messages.DEVICE_TYPE_NOT_FOUND, deviceTypeId), NOT_FOUND.getStatusCode());
        }
        logger.debug("Device type with id = {} does not exists any more.", deviceTypeId);
        webSocketClientHandler.sendMessage(request, new WebSocketResponse(), session);
    }

}
