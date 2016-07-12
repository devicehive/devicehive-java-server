package com.devicehive.resource.impl;


import com.devicehive.configuration.Messages;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.resource.DeviceClassResource;
import com.devicehive.resource.converters.SortOrderQueryParamParser;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.DeviceClassService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.List;

import static com.devicehive.configuration.Constants.ID;
import static com.devicehive.configuration.Constants.NAME;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;
import static javax.ws.rs.core.Response.Status.*;

/**
 * {@inheritDoc}
 */
@Service
public class DeviceClassResourceImpl implements DeviceClassResource {
    private static final Logger logger = LoggerFactory.getLogger(DeviceClassResourceImpl.class);

    @Autowired
    private DeviceClassService deviceClassService;


    /**
     * {@inheritDoc}
     */
    @Override
    public Response getDeviceClassList(String name, String namePattern, String version, String sortField, String sortOrderSt, Integer take, Integer skip) {
        logger.debug("DeviceClass list requested");
        boolean sortOrder = SortOrderQueryParamParser.parse(sortOrderSt);
        if (sortField != null && !ID.equalsIgnoreCase(sortField) && !NAME.equalsIgnoreCase(sortField)) {
            logger.debug("DeviceClass list request failed. Bad request for sortField");
            return ResponseFactory.response(Response.Status.BAD_REQUEST, new ErrorResponse(BAD_REQUEST.getStatusCode(),
                    Messages.INVALID_REQUEST_PARAMETERS));
        } else if (sortField != null) {
            sortField = sortField.toLowerCase();
        }

        List<DeviceClass> result = deviceClassService.getDeviceClassList(name, namePattern, version, sortField,
                sortOrder, take, skip);
        logger.debug("DeviceClass list proceed result. Result list contains {} elements", result.size());

        return ResponseFactory.response(OK, result, DEVICECLASS_LISTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response getDeviceClass(String id) {
        logger.debug("Get device class by id requested");

        DeviceClass result = deviceClassService.getWithEquipment(id);

        if (result == null) {
            logger.info("No device class with id = {} found", id);
            return ResponseFactory.response(NOT_FOUND,
                    new ErrorResponse(NOT_FOUND.getStatusCode(),
                            String.format(Messages.DEVICE_CLASS_NOT_FOUND, id)));
        }

        logger.debug("Requested device class found");

        return ResponseFactory.response(OK, result, DEVICECLASS_PUBLISHED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response insertDeviceClass(DeviceClass insert) {
        logger.debug("Insert device class requested");
        DeviceClass result = deviceClassService.addDeviceClass(insert);

        logger.debug("Device class inserted");
        return ResponseFactory.response(CREATED, result, DEVICECLASS_SUBMITTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response updateDeviceClass(String name, DeviceClassUpdate insert) {
        logger.debug("Device class update requested for id {}", name);
        deviceClassService.update(name, insert);
        logger.debug("Device class updated. Id {}", name);
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response deleteDeviceClass(String name) {
        logger.debug("Device class delete requested");
        deviceClassService.delete(name);
        logger.debug("Device class deleted");
        return ResponseFactory.response(NO_CONTENT);
    }

}