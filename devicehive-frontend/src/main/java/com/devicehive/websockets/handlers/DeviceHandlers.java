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
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.service.DeviceService;
import com.devicehive.vo.DeviceVO;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_PUBLISHED;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

@Component
public class DeviceHandlers {
    private static final Logger logger = LoggerFactory.getLogger(DeviceHandlers.class);

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private Gson gson;


    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_DEVICE')")
    public WebSocketResponse processDeviceGet(JsonObject request) {
        final String deviceId = Optional.ofNullable(request.get(Constants.DEVICE_ID))
                .map(JsonElement::getAsString)
                .orElse(null);
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
        return response;
    }

    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_DEVICE')")
    public WebSocketResponse processDeviceList(JsonObject request) {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        WebSocketResponse response = new WebSocketResponse();

        Set<String> deviceIds = deviceService.getDeviceIds(principal);
        List<DeviceVO> toResponse = deviceService.findByIdWithPermissionsCheck(deviceIds, principal);
        response.addValue(Constants.DEVICES, toResponse, DEVICE_PUBLISHED);

        return response;
    }

    @PreAuthorize("isAuthenticated() and hasPermission(null, 'REGISTER_DEVICE')")
    public WebSocketResponse processDeviceSave(JsonObject request,
                                               WebSocketSession session) {
        DeviceUpdate device = gson.fromJson(request.get(Constants.DEVICE), DeviceUpdate.class);
        String deviceId = device.getId().orElse(null);

        logger.debug("device/save process started for session {}", session.getId());
        if (deviceId == null) {
            throw new HiveException(Messages.DEVICE_ID_REQUIRED, SC_BAD_REQUEST);
        }
        deviceService.deviceSaveAndNotify(device, (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        logger.debug("device/save process ended for session  {}", session.getId());
        return new WebSocketResponse();
    }
}
