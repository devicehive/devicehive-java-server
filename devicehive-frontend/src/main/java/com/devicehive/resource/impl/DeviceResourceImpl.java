package com.devicehive.resource.impl;

/*
 * #%L
 * DeviceHive Java Server Common business logic
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
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.resource.DeviceResource;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.DeviceService;
import com.devicehive.vo.DeviceVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.Collections;

import static com.devicehive.configuration.Constants.*;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_PUBLISHED;
import static javax.ws.rs.core.Response.Status.*;

/**
 * {@inheritDoc}
 */
@Service
public class DeviceResourceImpl implements DeviceResource {

    private static final Logger logger = LoggerFactory.getLogger(DeviceResourceImpl.class);

    private final DeviceService deviceService;

    @Autowired
    public DeviceResourceImpl(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void list(String name, String namePattern, Long networkId, String networkName,
                     String sortField, String sortOrder, Integer take,
                     Integer skip, @Suspended final AsyncResponse asyncResponse) {

        logger.debug("Device list requested");

        if (sortField != null
                && !NAME.equalsIgnoreCase(sortField)
                && !STATUS.equalsIgnoreCase(sortField)
                && !NETWORK.equalsIgnoreCase(sortField)) {
            final Response response = ResponseFactory.response(BAD_REQUEST,
                    new ErrorResponse(BAD_REQUEST.getStatusCode(),
                            Messages.INVALID_REQUEST_PARAMETERS));
            asyncResponse.resume(response);
        } else if (sortField != null) {
            sortField = sortField.toLowerCase();
        }
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!principal.areAllNetworksAvailable() && (principal.getNetworkIds() == null || principal.getNetworkIds().isEmpty()) &&
                !principal.areAllDevicesAvailable() && (principal.getDeviceIds() == null || principal.getDeviceIds().isEmpty())) {
            logger.warn("Unable to get list for empty devices");
            final Response response = ResponseFactory.response(Response.Status.OK, Collections.<DeviceVO>emptyList(), JsonPolicyDef.Policy.DEVICE_PUBLISHED);
            asyncResponse.resume(response);
        } else {
            deviceService.list(name, namePattern, networkId, networkName, sortField, sortOrder, take, skip, principal)
                    .thenApply(devices -> {
                        logger.debug("Device list request proceed successfully");
                        return ResponseFactory.response(Response.Status.OK, devices, JsonPolicyDef.Policy.DEVICES_LISTED);
                    }).thenAccept(asyncResponse::resume);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(DeviceUpdate deviceUpdate, String deviceId, @Suspended final AsyncResponse asyncResponse) {
        if (deviceUpdate == null){
            final Response response = ResponseFactory.response(BAD_REQUEST,
                    new ErrorResponse(BAD_REQUEST.getStatusCode(),"Error! Validation failed: \nObject is null")
            );
            asyncResponse.resume(response);
        }
        logger.debug("Device register method requested. Device ID : {}, Device: {}", deviceId, deviceUpdate);

        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        deviceService.deviceSaveAndNotify(deviceId, deviceUpdate, principal).thenAccept(actionName -> {
            logger.debug("Device register finished successfully. Device ID: {}", deviceId);
            final Response response = ResponseFactory.response(Response.Status.NO_CONTENT);
            asyncResponse.resume(response);
        });
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response get(String deviceId) {
        logger.debug("Device get requested. Device ID: {}", deviceId);

        DeviceVO device = deviceService.findById(deviceId);

        if (device == null) {
            logger.error("device/get proceed with error. No Device with Device ID = {} found.", deviceId);
            ErrorResponse errorResponseEntity = new ErrorResponse(NOT_FOUND.getStatusCode(),
                    String.format(Messages.DEVICE_NOT_FOUND, deviceId));
            return ResponseFactory.response(NOT_FOUND, errorResponseEntity);
        }

        logger.debug("Device get proceed successfully. Device ID: {}", deviceId);
        return ResponseFactory.response(Response.Status.OK, device, DEVICE_PUBLISHED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response delete(String deviceId) {
        if (deviceId == null) {
            logger.error("device/get proceed with error. Device ID should be provided.");
            ErrorResponse errorResponseEntity = new ErrorResponse(BAD_REQUEST.getStatusCode(),
                    Messages.DEVICE_ID_REQUIRED);
            return ResponseFactory.response(BAD_REQUEST, errorResponseEntity);
        }
        
        boolean isDeviceDeleted = deviceService.deleteDevice(deviceId);
        if (!isDeviceDeleted) {
            logger.error("Delete device proceed with error. No Device with Device ID = {} found.", deviceId);
            ErrorResponse errorResponseEntity = new ErrorResponse(NOT_FOUND.getStatusCode(),
                    String.format(Messages.DEVICE_NOT_FOUND, deviceId));
            return ResponseFactory.response(NOT_FOUND, errorResponseEntity);
        }
        
        logger.debug("Device with id = {} is deleted", deviceId);
        return ResponseFactory.response(NO_CONTENT);
    }
}
