package com.devicehive.resource.impl;

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
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.List;
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
    public Response list(String name, String namePattern, String status, Long networkId, String networkName, Long deviceClassId, String deviceClassName,
                         String deviceClassVersion, String sortField, String sortOrderSt, Integer take, Integer skip) {

        logger.debug("Device list requested");

        boolean sortOrder = SortOrderQueryParamParser.parse(sortOrderSt);
        if (sortField != null
            && !NAME.equalsIgnoreCase(sortField)
            && !STATUS.equalsIgnoreCase(sortField)
            && !NETWORK.equalsIgnoreCase(sortField)
            && !DEVICE_CLASS.equalsIgnoreCase(sortField)) {
            return ResponseFactory.response(Response.Status.BAD_REQUEST,
                    new ErrorResponse(BAD_REQUEST.getStatusCode(),
                            Messages.INVALID_REQUEST_PARAMETERS));
        } else if (sortField != null) {
            sortField = sortField.toLowerCase();
        }
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        List<Device> result = deviceService.getList(name, namePattern, status, networkId, networkName, deviceClassId,
                                                    deviceClassName, deviceClassVersion, sortField, sortOrder, take,
                                                    skip, principal);

        logger.debug("Device list proceed result. Result list contains {} elems", result.size());

        return ResponseFactory.response(Response.Status.OK, ImmutableSet.copyOf(result), JsonPolicyDef.Policy.DEVICE_PUBLISHED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response register(DeviceUpdate deviceUpdate, String deviceGuid) {
        logger.debug("Device register method requested. Guid : {}, Device: {}", deviceGuid, deviceUpdate);

        deviceUpdate.setGuid(new NullableWrapper<>(deviceGuid));

        // TODO: [#98] refactor this API to have a separate endpoint for equipment update.
        Set<Equipment> equipmentSet = null;

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

        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Device device = deviceService.getDeviceWithNetworkAndDeviceClass(guid, principal);

        logger.debug("Device get proceed successfully. Guid {}", guid);
        return ResponseFactory.response(Response.Status.OK, device, DEVICE_PUBLISHED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response delete(String guid) {

        logger.debug("Device delete requested");
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final Device device = deviceService.findByGuidWithPermissionsCheck(guid, principal);
        if (device == null || !guid.equals(device.getGuid())) {
            logger.debug("No device found for guid : {}", guid);
            return ResponseFactory
                    .response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(),
                                    String.format(Messages.DEVICE_NOT_FOUND, guid)));
        }

        deviceService.deleteDevice(guid, principal);

        logger.debug("Device with id = {} is deleted", guid);

        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response equipment(String guid) {
        logger.debug("Device equipment requested for device {}", guid);

        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Device device = deviceService.getDeviceWithNetworkAndDeviceClass(guid, principal);
        List<DeviceEquipment> equipments = deviceEquipmentService.findByFK(device);

        logger.debug("Device equipment request proceed successfully for device {}", guid);

        return ResponseFactory.response(OK, equipments, DEVICE_EQUIPMENT_SUBMITTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response equipmentByCode(String guid, String code) {

        logger.debug("Device equipment by code requested");
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Device device = deviceService.getDeviceWithNetworkAndDeviceClass(guid, principal);

        DeviceEquipment equipment = deviceEquipmentService.findByCodeAndDevice(code, device);
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
