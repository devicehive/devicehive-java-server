package com.devicehive.resource.impl;


import com.devicehive.configuration.Messages;
import com.devicehive.resource.EquipmentResource;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.Equipment;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.updates.EquipmentUpdate;
import com.devicehive.service.DeviceClassService;
import com.devicehive.service.EquipmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.devicehive.configuration.Constants.DEVICE_CLASS_ID;
import static com.devicehive.configuration.Constants.ID;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.EQUIPMENTCLASS_SUBMITTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.EQUIPMENT_PUBLISHED;
import static javax.ws.rs.core.Response.Status.*;

@Service
@Path("/device/class/{deviceClassId}/equipment")
public class EquipmentResourceImpl implements EquipmentResource {
    private static final Logger logger = LoggerFactory.getLogger(EquipmentResourceImpl.class);

    @Autowired
    private DeviceClassService deviceClassService;

    @Autowired
    private EquipmentService equipmentService;


    /**
     * {@inheritDoc}
     */
    @Override
    public Response getEquipment(long classId, long eqId) {

        logger.debug("Device class's equipment get requested");
        Equipment result = equipmentService.getByDeviceClass(classId, eqId);

        if (result == null) {
            logger.debug("No equipment with id = {} for device class with id = {} found", eqId, classId);
            return ResponseFactory.response(NOT_FOUND,
                                            new ErrorResponse(NOT_FOUND.getStatusCode(),
                                                              String.format(Messages.EQUIPMENT_NOT_FOUND, eqId,
                                                                            classId)));
        }
        logger.debug("Device class's equipment get proceed successfully");

        return ResponseFactory.response(OK, result, EQUIPMENT_PUBLISHED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response insertEquipment(long classId, Equipment equipment) {

        logger.debug("Insert device class's equipment requested");
        Equipment result = deviceClassService.createEquipment(classId, equipment);
        logger.debug("New device class's equipment created");

        return ResponseFactory.response(CREATED, result, EQUIPMENTCLASS_SUBMITTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response updateEquipment(long classId, long eqId, EquipmentUpdate equipmentUpdate) {

        logger.debug("Update device class's equipment requested");

        if (!equipmentService.update(equipmentUpdate, eqId, classId)) {
            logger.debug("Unable to update equipment. Equipment with id = {} for device class with id = {} not found",
                         eqId, classId);
            return ResponseFactory.response(NOT_FOUND,
                                            new ErrorResponse(NOT_FOUND.getStatusCode(),
                                                              String.format(Messages.EQUIPMENT_NOT_FOUND, eqId,
                                                                            classId)));
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
        equipmentService.delete(eqId, classId);
        logger.debug("Delete device class's equipment finished");

        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response getEquipment() {
        return ResponseFactory.response(METHOD_NOT_ALLOWED, new ErrorResponse(METHOD_NOT_ALLOWED.getStatusCode(), METHOD_NOT_ALLOWED.getReasonPhrase()));
    }

}
