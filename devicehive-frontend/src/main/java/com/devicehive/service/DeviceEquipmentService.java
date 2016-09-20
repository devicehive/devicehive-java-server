package com.devicehive.service;

import com.devicehive.dao.DeviceEquipmentDao;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.SpecialNotifications;
import com.devicehive.service.time.TimestampService;
import com.devicehive.util.ServerResponsesFactory;
import com.devicehive.vo.DeviceEquipmentVO;
import com.devicehive.vo.DeviceVO;
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
    private TimestampService timestampService;
    @Autowired
    private DeviceEquipmentDao deviceEquipmentDao;

    /**
     * find Device equipment by device
     *
     * @param device Equipment will be fetched for this device
     * @return List of DeviceEquipment for specified device
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<DeviceEquipmentVO> findByFK(@NotNull DeviceVO device) {
        return deviceEquipmentDao.getByDevice(device);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public DeviceEquipmentVO findByCodeAndDevice(@NotNull String code, @NotNull DeviceVO device) {
        return deviceEquipmentDao.getByDeviceAndCode(code, device);
    }

    @Transactional
    public DeviceNotification refreshDeviceEquipment(DeviceNotification notificationMessage, DeviceVO device) {
        DeviceEquipmentVO deviceEquipment = null;
        if (notificationMessage.getNotification().equals(SpecialNotifications.EQUIPMENT)) {
            deviceEquipment = ServerResponsesFactory.parseDeviceEquipmentNotification(notificationMessage, device);
            if (deviceEquipment.getTimestamp() == null) {
                // TODO [rafa] why do we need timestamp here?
                deviceEquipment.setTimestamp(timestampService.getDate());
            }
        }
        createDeviceEquipment(deviceEquipment, device);
        return notificationMessage;
    }

    @Transactional
    public void createDeviceEquipment(DeviceEquipmentVO deviceEquipment, DeviceVO device) {
        DeviceEquipmentVO equipment = findByCodeAndDevice(deviceEquipment.getCode(), device);
        if (equipment != null) {
            equipment.setTimestamp(timestampService.getDate());
            equipment.setParameters(deviceEquipment.getParameters());
            deviceEquipmentDao.merge(equipment, device);
        } else {
            deviceEquipment.setTimestamp(timestampService.getDate());
            deviceEquipmentDao.persist(deviceEquipment, device);
        }
    }
}
