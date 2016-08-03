package com.devicehive.dao;

import com.devicehive.model.Device;
import com.devicehive.model.DeviceEquipment;
import com.devicehive.vo.DeviceEquipmentVO;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Created by Gleb on 07.07.2016.
 */
public interface DeviceEquipmentDao {

    List<DeviceEquipmentVO> getByDevice(Device device);

    DeviceEquipmentVO getByDeviceAndCode(@NotNull String code, @NotNull Device device);

    DeviceEquipmentVO merge(DeviceEquipmentVO deviceEquipment, Device device);

    void persist(DeviceEquipmentVO deviceEquipment, Device device);
}
