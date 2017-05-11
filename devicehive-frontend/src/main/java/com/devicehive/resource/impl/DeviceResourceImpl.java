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
import com.devicehive.model.*;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.resource.DeviceResource;
import com.devicehive.resource.converters.SortOrderQueryParamParser;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.DeviceEquipmentService;
import com.devicehive.service.DeviceService;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.DeviceClassEquipmentVO;
import com.devicehive.vo.DeviceEquipmentVO;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.devicehive.configuration.Constants.*;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_EQUIPMENT_SUBMITTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_PUBLISHED;
import static javax.ws.rs.core.Response.Status.*;

/**
 * {@inheritDoc}
 */
@Service
public class DeviceResourceImpl implements DeviceResource {
    private static final Logger logger = LoggerFactory.getLogger(DeviceResourceImpl.class);

    @Autowired
    private DeviceEquipmentService deviceEquipmentService;
    @Autowired
    private DeviceService deviceService;

    /**
     * {@inheritDoc}
     */
    @Override
    public void list(String name, String namePattern, Long networkId, String networkName,
                     Long deviceClassId, String deviceClassName, String sortField, String sortOrderSt, Integer take,
                     Integer skip, @Suspended final AsyncResponse asyncResponse) {

        logger.debug("Device list requested");

        boolean sortOrder = SortOrderQueryParamParser.parse(sortOrderSt);
        if (sortField != null
                && !NAME.equalsIgnoreCase(sortField)
                && !STATUS.equalsIgnoreCase(sortField)
                && !NETWORK.equalsIgnoreCase(sortField)
                && !DEVICE_CLASS.equalsIgnoreCase(sortField)) {
            final Response response = ResponseFactory.response(BAD_REQUEST,
                    new ErrorResponse(BAD_REQUEST.getStatusCode(),
                            Messages.INVALID_REQUEST_PARAMETERS));
            asyncResponse.resume(response);
        } else if (sortField != null) {
            sortField = sortField.toLowerCase();
        }
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        deviceService.list(name, namePattern, networkId, networkName, deviceClassId,
                deviceClassName, sortField, sortOrder, take, skip, principal)
                .thenApply(devices -> {
                    logger.debug("Device list proceed result. Result list contains {} elems", devices.size());
                    return ResponseFactory.response(Response.Status.OK, ImmutableSet.copyOf(devices), JsonPolicyDef.Policy.DEVICE_PUBLISHED);
                }).thenAccept(asyncResponse::resume);
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

        // TODO: [#98] refactor this API to have a separate endpoint for equipment update.
        Set<DeviceClassEquipmentVO> equipmentSet = new HashSet<>();

        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        deviceService.deviceSaveAndNotify(deviceUpdate, equipmentSet, principal);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Response equipment(String guid) {
        logger.debug("Device equipment requested for device {}", guid);

        DeviceVO device = deviceService.findById(guid);
        List<DeviceEquipmentVO> equipments = deviceEquipmentService.findByFK(device);

        logger.debug("Device equipment request proceed successfully for device {}", guid);

        return ResponseFactory.response(OK, equipments, DEVICE_EQUIPMENT_SUBMITTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response equipmentByCode(String guid, String code) {
        logger.debug("Device equipment by code requested");
        DeviceVO device = deviceService.findById(guid);

        DeviceEquipmentVO equipment = deviceEquipmentService.findByCodeAndDevice(code, device);
        if (equipment == null) {
            logger.debug("No device equipment found for code : {} and guid : {}", code, guid);
            return ResponseFactory
                    .response(NOT_FOUND,
                            new ErrorResponse(NOT_FOUND.getStatusCode(),
                                    String.format(Messages.DEVICE_NOT_FOUND, guid)));
        }
        logger.debug("Device equipment by code proceed successfully");

        return ResponseFactory.response(OK, equipment, DEVICE_EQUIPMENT_SUBMITTED);
    }


}
