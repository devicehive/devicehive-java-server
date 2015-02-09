package com.devicehive.service;

import com.devicehive.dao.DeviceEquipmentDAO;
import com.devicehive.model.*;
import com.devicehive.util.ServerResponsesFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.validation.constraints.NotNull;
import java.util.List;

//TODO:javadoc
@Stateless
public class DeviceEquipmentService {

    @EJB
    private DeviceEquipmentDAO deviceEquipmentDAO;
    @EJB
    private TimestampService timestampService;


    /**
     * find Device equipment by device
     *
     * @param device Equipment will be fetched for this device
     * @return List of DeviceEquipment for specified device
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DeviceEquipment> findByFK(@NotNull Device device) {
        return deviceEquipmentDAO.findByFK(device);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public DeviceEquipment findByCodeAndDevice(@NotNull String code, @NotNull Device device) {
        return deviceEquipmentDAO.findByCodeAndDevice(code, device);
    }

    public void createDeviceEquipment(DeviceEquipment deviceEquipment) {
        if (deviceEquipment != null && !deviceEquipmentDAO.update(deviceEquipment)) {
            deviceEquipment.setTimestamp(timestampService.getTimestamp());
            deviceEquipmentDAO.createDeviceEquipment(deviceEquipment);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public DeviceNotificationMessage refreshDeviceEquipment(DeviceNotificationMessage notificationMessage, Device device) {
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
