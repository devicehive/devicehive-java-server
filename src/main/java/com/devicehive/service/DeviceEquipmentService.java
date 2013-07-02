package com.devicehive.service;

import com.devicehive.dao.DeviceEquipmentDAO;
import com.devicehive.model.DeviceEquipment;

import javax.inject.Inject;
import javax.transaction.Transactional;

public class DeviceEquipmentService {

    @Inject
    private DeviceEquipmentDAO deviceEquipmentDAO;

    @Transactional
    public void resolveSaveOrUpdateEquipment(DeviceEquipment deviceEquipment) {
        DeviceEquipment existingDeviceEquipment = deviceEquipmentDAO.findByCode(deviceEquipment.getCode());
        if (existingDeviceEquipment == null) {
            deviceEquipmentDAO.saveDeviceEquipment(deviceEquipment);
        } else {
            existingDeviceEquipment.setParameters(deviceEquipment.getParameters());
            existingDeviceEquipment.setDevice(deviceEquipment.getDevice());
            deviceEquipmentDAO.updateDeviceEquipment(existingDeviceEquipment);
        }
    }
}
