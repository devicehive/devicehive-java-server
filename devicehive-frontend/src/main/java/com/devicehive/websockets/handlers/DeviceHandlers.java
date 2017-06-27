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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_PUBLISHED;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;

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

        if (deviceId != null) {
            DeviceVO toResponse = deviceService.findByIdWithPermissionsCheck(deviceId, principal);
            response.addValue(Constants.DEVICE, toResponse, DEVICE_PUBLISHED);
            return response;
        } else {
            Set<String> deviceIds = principal.getDeviceIds();
            if (principal.areAllDevicesAvailable()) {
                try {
                    deviceIds = deviceService.list(null, null, null, null,
                            null,false, null, null, principal)
                            .get()
                            .stream()
                            .map(deviceVO -> deviceVO.getDeviceId())
                            .collect(Collectors.toSet());
                } catch (Exception e) {
                    logger.error(Messages.INTERNAL_SERVER_ERROR, e);
                    throw new HiveException(Messages.INTERNAL_SERVER_ERROR, SC_INTERNAL_SERVER_ERROR);
                }
            }
            List<DeviceVO> toResponse = deviceService.findByIdWithPermissionsCheck(deviceIds, principal);
            response.addValue(Constants.DEVICE, toResponse, DEVICE_PUBLISHED);
            
            return response;
        }
    }

    @PreAuthorize("isAuthenticated() and hasPermission(null, 'REGISTER_DEVICE')")
    public WebSocketResponse processDeviceSave(JsonObject request,
                                               WebSocketSession session) {
        String deviceId = request.get(Constants.DEVICE_ID).getAsString();
        DeviceUpdate device = gson.fromJson(request.get(Constants.DEVICE), DeviceUpdate.class);

        logger.debug("device/save process started for session {}", session.getId());
        if (deviceId == null) {
            throw new HiveException(Messages.DEVICE_ID_REQUIRED, SC_BAD_REQUEST);
        }
        device.setId(deviceId);
        deviceService.deviceSaveAndNotify(device, (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        logger.debug("device/save process ended for session  {}", session.getId());
        return new WebSocketResponse();
    }
}
