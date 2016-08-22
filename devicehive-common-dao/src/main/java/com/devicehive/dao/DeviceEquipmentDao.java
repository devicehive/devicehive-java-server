package com.devicehive.dao;

import com.devicehive.vo.DeviceEquipmentVO;
import com.devicehive.vo.DeviceVO;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Created by Gleb on 07.07.2016.
 */
public interface DeviceEquipmentDao {

    List<DeviceEquipmentVO> getByDevice(@NotNull DeviceVO device);

    DeviceEquipmentVO getByDeviceAndCode(@NotNull String code, @NotNull DeviceVO device);

    DeviceEquipmentVO merge(DeviceEquipmentVO deviceEquipment, DeviceVO device);

    void persist(DeviceEquipmentVO deviceEquipment, DeviceVO device);
}
