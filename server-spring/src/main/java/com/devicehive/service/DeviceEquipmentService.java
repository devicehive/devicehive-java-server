package com.devicehive.service;

import com.devicehive.dao.DeviceEquipmentDAO;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceEquipment;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.SpecialNotifications;
import com.devicehive.util.ServerResponsesFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.List;

//TODO:javadoc
@Component
public class DeviceEquipmentService {

    @Autowired
    private DeviceEquipmentDAO deviceEquipmentDAO;
    @Autowired
    private TimestampService timestampService;


    /**
     * find Device equipment by device
     *
     * @param device Equipment will be fetched for this device
     * @return List of DeviceEquipment for specified device
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<DeviceEquipment> findByFK(@NotNull Device device) {
        return deviceEquipmentDAO.findByFK(device);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public DeviceEquipment findByCodeAndDevice(@NotNull String code, @NotNull Device device) {
        return deviceEquipmentDAO.findByCodeAndDevice(code, device);
    }

    @Transactional
    public void createDeviceEquipment(DeviceEquipment deviceEquipment) {
        if (deviceEquipment != null && !deviceEquipmentDAO.update(deviceEquipment)) {
            deviceEquipment.setTimestamp(timestampService.getTimestamp());
            deviceEquipmentDAO.createDeviceEquipment(deviceEquipment);
        }
    }

    @Transactional
    public DeviceNotification refreshDeviceEquipment(DeviceNotification notificationMessage, Device device) {
        DeviceEquipment deviceEquipment = null;
        if (notificationMessage.getNotification().equals(SpecialNotifications.EQUIPMENT)) {
            deviceEquipment = ServerResponsesFactory.parseDeviceEquipmentNotification(notificationMessage, device);
            if (deviceEquipment.getTimestamp() == null) {
                deviceEquipment.setTimestamp(timestampService.getTimestamp());
            }
        }
        createDeviceEquipment(deviceEquipment);
        return notificationMessage;
    }
}
