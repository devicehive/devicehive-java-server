package com.devicehive.service;

import com.devicehive.dao.DeviceEquipmentDAO;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceEquipment;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.SpecialNotifications;
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
    private DeviceEquipmentDAO deviceEquipmentDAO;
    private TimestampService timestampService;

    @EJB
    public void setDeviceEquipmentDAO(DeviceEquipmentDAO deviceEquipmentDAO) {
        this.deviceEquipmentDAO = deviceEquipmentDAO;
    }

    @EJB
    public void setTimestampService(TimestampService timestampService) {
        this.timestampService = timestampService;
    }

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
    public DeviceNotification refreshDeviceEquipment(DeviceNotification notification, Device device) {
        DeviceEquipment deviceEquipment = null;
        if (notification.getNotification().equals(SpecialNotifications.EQUIPMENT)) {
            deviceEquipment = ServerResponsesFactory.parseDeviceEquipmentNotification(notification, device);
            if (deviceEquipment.getTimestamp() == null) {
                deviceEquipment.setTimestamp(timestampService.getTimestamp());
            }
        }
        createDeviceEquipment(deviceEquipment);
        return notification;
    }
}
