package com.devicehive.client.util;


import com.devicehive.client.model.Device;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.Equipment;
import com.devicehive.client.model.exceptions.HiveClientException;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

/**
 * Class to perform pre-validation of required fields and possible values of the fields.
 */
public class HiveValidator {

    public static void validate(Device device) {
        List<String> constraintViolations = new LinkedList<>();
        if (StringUtils.isBlank(device.getId())) {
            constraintViolations.add("id is required");
        }
        if (device.getKey() == null) {
            constraintViolations.add("key is required");
        }
        if (StringUtils.isBlank(device.getName())) {
            constraintViolations.add("name is required");
        }
        if (device.getDeviceClass() == null) {
            constraintViolations.add("device class is required");
        } else {
            if (StringUtils.isBlank(device.getDeviceClass().getName())) {
                constraintViolations.add("device class version is required");
            }
            if (StringUtils.isBlank(device.getDeviceClass().getVersion())) {
                constraintViolations.add("device class version is required");
            }
        }
        if (device.getNetwork() != null && StringUtils.isBlank(device.getNetwork().getName())) {
            constraintViolations.add("network name is required");
        }
        if (device.getDeviceClass().getEquipment() != null) {
            for (Equipment eq : device.getDeviceClass().getEquipment()) {
                if (eq != null) {
                    if (StringUtils.isBlank(eq.getName())) {
                        constraintViolations.add("equipment name is required");
                    }
                    if (StringUtils.isBlank(eq.getCode())) {
                        constraintViolations.add("equipment code is required");
                    }
                    if (StringUtils.isBlank(eq.getType())) {
                        constraintViolations.add("equipment type is required");
                    }
                }
            }
        }
        if (!constraintViolations.isEmpty()) {
            String message = "Validation failed with following constraint violations: ";
            throw new HiveClientException(message + StringUtils.join(constraintViolations, ";"),
                    BAD_REQUEST.getStatusCode());
        }
    }

    public static void validate(DeviceNotification deviceNotification) {
        if (StringUtils.isEmpty(deviceNotification.getNotification())) {
            throw new HiveClientException("Device notification name is required!", BAD_REQUEST.getStatusCode());
        }
    }
}
