package com.devicehive.resource.impl;

import com.devicehive.configuration.Messages;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.resource.DeviceClassResource;
import com.devicehive.resource.converters.SortOrderQueryParamParser;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.DeviceClassService;
import com.devicehive.vo.DeviceClassWithEquipmentVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;

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
    public void getDeviceClassList(String name, String namePattern, String sortField, String sortOrderSt,
                                   Integer take, Integer skip, @Suspended final AsyncResponse asyncResponse) {
        logger.debug("DeviceClass list requested");
        boolean sortOrder = SortOrderQueryParamParser.parse(sortOrderSt);

        if (sortField != null && !ID.equalsIgnoreCase(sortField) && !NAME.equalsIgnoreCase(sortField)) {
            logger.debug("DeviceClass list request failed. Bad request for sortField");
            final Response response = ResponseFactory.response(Response.Status.BAD_REQUEST,
                    new ErrorResponse(BAD_REQUEST.getStatusCode(),
                            Messages.INVALID_REQUEST_PARAMETERS));
            asyncResponse.resume(response);
        } else {
            if (sortField != null) {
                sortField = sortField.toLowerCase();
            }

            deviceClassService.list(name, namePattern, sortField, sortOrder, take, skip)
                    .thenApply(deviceClasses -> {
                        logger.debug("DeviceClass list proceed result. Result list contains {} elements", deviceClasses.size());
                        return ResponseFactory.response(OK, deviceClasses, DEVICECLASS_LISTED);
                    })
                    .thenAccept(asyncResponse::resume);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response getDeviceClass(long id) {
        logger.debug("Get device class by id requested");
        DeviceClassWithEquipmentVO result = deviceClassService.getWithEquipment(id);
        if (result == null) {
            logger.info("No device class with id = {} found", id);
            return ResponseFactory.response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(Messages.DEVICE_CLASS_NOT_FOUND, id)));
        }
        logger.debug("Requested device class found");
        return ResponseFactory.response(OK, result, DEVICECLASS_PUBLISHED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response insertDeviceClass(DeviceClassWithEquipmentVO insert) {
        logger.debug("Insert device class requested");
        DeviceClassWithEquipmentVO result = deviceClassService.addDeviceClass(insert);

        logger.debug("Device class inserted");
        return ResponseFactory.response(CREATED, result, DEVICECLASS_SUBMITTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response updateDeviceClass(long id, DeviceClassUpdate insert) {
        logger.debug("Device class update requested for id {}", id);
        deviceClassService.update(id, insert);
        logger.debug("Device class updated. Id {}", id);
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response deleteDeviceClass(long id) {
        logger.debug("Device class delete requested");
        deviceClassService.delete(id);
        logger.debug("Device class deleted");
        return ResponseFactory.response(NO_CONTENT);
    }

}