package com.devicehive.service;

import com.devicehive.dao.DeviceEquipmentDao;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceEquipment;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.SpecialNotifications;
import com.devicehive.service.time.TimestampService;
import com.devicehive.util.ServerResponsesFactory;
import com.devicehive.vo.DeviceEquipmentVO;
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
    public List<DeviceEquipmentVO> findByFK(@NotNull Device device) {
        return deviceEquipmentDao.getByDevice(device);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public DeviceEquipmentVO findByCodeAndDevice(@NotNull String code, @NotNull Device device) {
        return deviceEquipmentDao.getByDeviceAndCode(code, device);
    }

    @Transactional
    public DeviceNotification refreshDeviceEquipment(DeviceNotification notificationMessage, Device device) {
        DeviceEquipmentVO deviceEquipment = null;
        if (notificationMessage.getNotification().equals(SpecialNotifications.EQUIPMENT)) {
            deviceEquipment = ServerResponsesFactory.parseDeviceEquipmentNotification(notificationMessage, device);
            if (deviceEquipment.getTimestamp() == null) {
                // TODO [rafa] why do we need timestamp here?
                deviceEquipment.setTimestamp(timestampService.getTimestamp());
            }
        }
        createDeviceEquipment(deviceEquipment, device);
        return notificationMessage;
    }

    @Transactional
    public void createDeviceEquipment(DeviceEquipmentVO deviceEquipment, Device device) {
        DeviceEquipmentVO equipment = findByCodeAndDevice(deviceEquipment.getCode(), device);
        if (equipment != null) {
            equipment.setTimestamp(timestampService.getTimestamp());
            equipment.setParameters(deviceEquipment.getParameters());
            deviceEquipmentDao.merge(equipment, device);
        } else {
            deviceEquipment.setTimestamp(timestampService.getTimestamp());
            deviceEquipmentDao.persist(deviceEquipment, device);
        }
    }
}
