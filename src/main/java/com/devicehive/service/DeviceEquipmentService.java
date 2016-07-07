package com.devicehive.service;

import com.devicehive.dao.CacheConfig;
import com.devicehive.dao.rdbms.GenericDaoImpl;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceEquipment;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.SpecialNotifications;
import com.devicehive.service.time.TimestampService;
import com.devicehive.util.ServerResponsesFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.List;

import static java.util.Optional.of;

//TODO:javadoc
@Component
public class DeviceEquipmentService {

    @Autowired
    private GenericDaoImpl genericDAO;
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
        return genericDAO.createNamedQuery(DeviceEquipment.class, "DeviceEquipment.getByDevice", of(CacheConfig.refresh()))
                .setParameter("device", device)
                .getResultList();
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public DeviceEquipment findByCodeAndDevice(@NotNull String code, @NotNull Device device) {
        return genericDAO.createNamedQuery(DeviceEquipment.class, "DeviceEquipment.getByDeviceAndCode", of(CacheConfig.refresh()))
                .setParameter("code", code)
                .setParameter("device", device)
                .getResultList()
                .stream().findFirst().orElse(null);
    }

    @Transactional
    public void createDeviceEquipment(DeviceEquipment deviceEquipment) {
        DeviceEquipment equipment = findByCodeAndDevice(deviceEquipment.getCode(), deviceEquipment.getDevice());
        if (equipment != null) {
            equipment.setTimestamp(timestampService.getTimestamp());
            equipment.setParameters(deviceEquipment.getParameters());
            genericDAO.merge(equipment);
        } else {
            deviceEquipment.setTimestamp(timestampService.getTimestamp());
            genericDAO.persist(deviceEquipment);
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
