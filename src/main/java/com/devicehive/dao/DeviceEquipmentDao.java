package com.devicehive.dao;

import com.devicehive.model.Device;
import com.devicehive.model.DeviceEquipment;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Created by Gleb on 07.07.2016.
 */
public interface DeviceEquipmentDao {
    List<DeviceEquipment> getByDevice(Device device);
    DeviceEquipment getByDeviceAndCode(@NotNull String code, @NotNull Device device);
}
