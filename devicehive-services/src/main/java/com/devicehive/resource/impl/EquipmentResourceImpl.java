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

import com.devicehive.configuration.Messages;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.updates.EquipmentUpdate;
import com.devicehive.resource.EquipmentResource;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.DeviceClassService;
import com.devicehive.vo.DeviceClassEquipmentVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.EQUIPMENTCLASS_SUBMITTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.EQUIPMENT_PUBLISHED;
import static javax.ws.rs.core.Response.Status.*;

@Service
public class EquipmentResourceImpl implements EquipmentResource {

    private static final Logger logger = LoggerFactory.getLogger(EquipmentResourceImpl.class);

    @Autowired
    private DeviceClassService deviceClassService;

    /**
     * {@inheritDoc}
     */
    @Override
    public Response getEquipment(long classId, long eqId) {
        logger.debug("Device class's equipment get requested");
        DeviceClassEquipmentVO result = deviceClassService.getByDeviceClass(classId, eqId);
        if (result == null) {
            logger.debug("No equipment with id = {} for device class with id = {} found", eqId, classId);
            return ResponseFactory.response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(Messages.EQUIPMENT_NOT_FOUND, eqId, classId)));
        }
        logger.debug("Device class's equipment get proceed successfully");
        return ResponseFactory.response(OK, result, EQUIPMENT_PUBLISHED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response insertEquipment(long classId, DeviceClassEquipmentVO equipment) {
        logger.debug("Insert device class's equipment requested");
        DeviceClassEquipmentVO result = deviceClassService.createEquipment(classId, equipment);
        logger.debug("New device class's equipment created");
        return ResponseFactory.response(CREATED, result, EQUIPMENTCLASS_SUBMITTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response updateEquipment(long classId, long eqId, EquipmentUpdate equipmentUpdate) {
        logger.debug("Update device class's equipment requested");
        boolean update = deviceClassService.update(equipmentUpdate, eqId, classId);
        if (!update) {
            logger.debug("Unable to update equipment. Equipment with id = {} for device class with id = {} not found", eqId, classId);
            return ResponseFactory.response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(Messages.EQUIPMENT_NOT_FOUND, eqId, classId)));
        }
        logger.debug("Update device class's equipment finished successfully");
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response deleteEquipment(long classId, long eqId) {
        logger.debug("Delete device class's equipment requested");
        deviceClassService.delete(eqId, classId);
        logger.debug("Delete device class's equipment finished");
        return ResponseFactory.response(NO_CONTENT);
    }

}
