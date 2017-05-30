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
import com.devicehive.resource.converters.SortOrderQueryParamParser;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.DeviceService;
import com.devicehive.vo.DeviceVO;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.*;

import static com.devicehive.configuration.Constants.*;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_PUBLISHED;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;

/**
 * {@inheritDoc}
 */
@Service
public class DeviceResourceImpl implements DeviceResource {
    private static final Logger logger = LoggerFactory.getLogger(DeviceResourceImpl.class);

    @Autowired
    private DeviceService deviceService;

    /**
     * {@inheritDoc}
     */
    @Override
    public void list(String name, String namePattern, Long networkId, String networkName,
                     String sortField, String sortOrderSt, Integer take,
                     Integer skip, @Suspended final AsyncResponse asyncResponse) {

        logger.debug("Device list requested");

        boolean sortOrder = SortOrderQueryParamParser.parse(sortOrderSt);
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

        if (!principal.areAllNetworksAvailable() && (principal.getNetworkIds() == null || principal.getNetworkIds().isEmpty()) ||
                !principal.areAllDevicesAvailable() && (principal.getDeviceGuids() == null || principal.getDeviceGuids().isEmpty())) {
            logger.warn("Unable to get list for empty devices");
            final Response response = ResponseFactory.response(Response.Status.OK, Collections.<DeviceVO>emptyList(), JsonPolicyDef.Policy.DEVICE_PUBLISHED);
            asyncResponse.resume(response);
        } else {
            deviceService.list(name, namePattern, networkId, networkName, sortField, sortOrder, take, skip, principal)
                    .thenApply(devices -> {
                        logger.debug("Device list proceed result. Result list contains {} elems", devices.size());
                        return ResponseFactory.response(Response.Status.OK, ImmutableSet.copyOf(devices), JsonPolicyDef.Policy.DEVICE_PUBLISHED);
                    }).thenAccept(asyncResponse::resume);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response register(DeviceUpdate deviceUpdate, String deviceGuid) {
        if (deviceUpdate == null){
            return ResponseFactory.response(
                    BAD_REQUEST,
                    new ErrorResponse(BAD_REQUEST.getStatusCode(),"Error! Validation failed: \nObject is null")
            );
        }
        logger.debug("Device register method requested. Guid : {}, Device: {}", deviceGuid, deviceUpdate);

        deviceUpdate.setGuid(deviceGuid);

        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        deviceService.deviceSaveAndNotify(deviceUpdate, principal);
        logger.debug("Device register finished successfully. Guid : {}", deviceGuid);

        return ResponseFactory.response(Response.Status.NO_CONTENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response get(String guid) {
        logger.debug("Device get requested. Guid {}", guid);

        DeviceVO device = deviceService.findById(guid);

        logger.debug("Device get proceed successfully. Guid {}", guid);
        return ResponseFactory.response(Response.Status.OK, device, DEVICE_PUBLISHED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response delete(String guid) {
        deviceService.deleteDevice(guid);
        logger.debug("Device with id = {} is deleted", guid);
        return ResponseFactory.response(NO_CONTENT);
    }
}
