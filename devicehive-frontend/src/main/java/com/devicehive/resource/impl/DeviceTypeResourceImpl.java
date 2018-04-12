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

import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Messages;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.updates.DeviceTypeUpdate;
import com.devicehive.resource.DeviceTypeResource;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.BaseDeviceTypeService;
import com.devicehive.service.DeviceTypeService;
import com.devicehive.vo.DeviceTypeVO;
import com.devicehive.vo.DeviceTypeWithUsersAndDevicesVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.Collections;

import static com.devicehive.configuration.Constants.ID;
import static com.devicehive.configuration.Constants.NAME;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_TYPES_LISTED;
import static javax.ws.rs.core.Response.Status.*;

@Service
public class DeviceTypeResourceImpl implements DeviceTypeResource {

    private static final Logger logger = LoggerFactory.getLogger(DeviceTypeResourceImpl.class);

    private final DeviceTypeService deviceTypeService;

    @Autowired
    public DeviceTypeResourceImpl(DeviceTypeService deviceTypeService) {
        this.deviceTypeService = deviceTypeService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void list(String name, String namePattern, String sortField, String sortOrder, Integer take, Integer skip,
                     @Suspended final AsyncResponse asyncResponse) {

        logger.debug("Device type list requested");

        if (sortField != null && !ID.equalsIgnoreCase(sortField) && !NAME.equalsIgnoreCase(sortField)) {
            logger.error("Unable to proceed device type list request. Invalid sortField");
            final Response response = ResponseFactory.response(BAD_REQUEST,
                    new ErrorResponse(BAD_REQUEST.getStatusCode(),
                            Messages.INVALID_REQUEST_PARAMETERS));
            asyncResponse.resume(response);
        } else if (sortField != null) {
            sortField = sortField.toLowerCase();
        }
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!principal.areAllDeviceTypesAvailable() && (principal.getDeviceTypeIds() == null || principal.getDeviceTypeIds().isEmpty())) {
            logger.warn("Unable to get list for empty device types");
            final Response response = ResponseFactory.response(OK, Collections.<DeviceTypeVO>emptyList(), DEVICE_TYPES_LISTED);
            asyncResponse.resume(response);
        } else {
            deviceTypeService.list(name, namePattern, sortField, sortOrder, take, skip, principal)
                    .thenApply(deviceTypes -> {
                        logger.debug("Device type list request proceed successfully.");
                        return ResponseFactory.response(OK, deviceTypes, DEVICE_TYPES_LISTED);
                    }).thenAccept(asyncResponse::resume);
        }
    }

    @Override
    public void count(String name, String namePattern, AsyncResponse asyncResponse) {
        logger.debug("Device type count requested");
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        deviceTypeService.count(name, namePattern, principal)
                .thenApply(count -> {
                    logger.debug("Device type count request proceed successfully.");
                    return ResponseFactory.response(OK, count, DEVICE_TYPES_LISTED);
                }).thenAccept(asyncResponse::resume);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response get(long id) {
        logger.debug("Device type get requested.");
        DeviceTypeWithUsersAndDevicesVO existing = deviceTypeService.getWithDevices(id);
        if (existing == null) {
            logger.error("Device type with id =  {} does not exists", id);
            return ResponseFactory.response(Response.Status.NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(),
                    String.format(Messages.DEVICE_TYPE_NOT_FOUND, id)));
        }
        return ResponseFactory.response(OK, existing, JsonPolicyDef.Policy.DEVICE_TYPE_PUBLISHED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response insert(DeviceTypeUpdate deviceType) {
        logger.debug("Device type insert requested");
        DeviceTypeVO result = deviceTypeService.create(deviceType.convertTo());
        logger.debug("New device type has been created");
        return ResponseFactory.response(CREATED, result, JsonPolicyDef.Policy.DEVICE_TYPE_SUBMITTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response update(DeviceTypeUpdate deviceTypeToUpdate, long id) {
        logger.debug("Device type update requested. Id : {}", id);
        deviceTypeService.update(id, deviceTypeToUpdate);
        logger.debug("Device type has been updated successfully. Id : {}", id);
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response delete(long id, boolean force) {
        logger.debug("Device type delete requested");
        boolean isDeleted = deviceTypeService.delete(id, force);
        if (!isDeleted) {
            logger.error(String.format(Messages.DEVICE_TYPE_NOT_FOUND, id));
            return ResponseFactory.response(NOT_FOUND,
                    new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(Messages.DEVICE_TYPE_NOT_FOUND, id)));
        }
        logger.debug("Device type with id = {} does not exists any more.", id);
        return ResponseFactory.response(NO_CONTENT);
    }
}