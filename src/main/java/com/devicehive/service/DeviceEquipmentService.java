package com.devicehive.service;

import com.devicehive.dao.DeviceEquipmentDAO;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceEquipment;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.List;

//TODO:javadoc
@Stateless
public class DeviceEquipmentService {

    @Inject
    private DeviceEquipmentDAO deviceEquipmentDAO;

    /**
     * find Device equipment by device
     * @param device Equipment will be fetched for this device
     * @return List of DeviceEquipment for specified device
     */
    public List<DeviceEquipment> findByFK(@NotNull Device device) {
        return deviceEquipmentDAO.findByFK(device);
    }

    public DeviceEquipment findByCodeAndDevice(@NotNull String code, @NotNull Device device){
        return deviceEquipmentDAO.findByCodeAndDevice(code, device);
    }
}
