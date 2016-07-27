package com.devicehive.dao;

import com.devicehive.vo.DeviceClassEquipmentVO;
import com.devicehive.vo.DeviceClassWithEquipmentVO;

import javax.validation.constraints.NotNull;
import java.util.List;

public interface DeviceClassDao {

    void remove(long id);

    DeviceClassWithEquipmentVO find(long id);

    DeviceClassWithEquipmentVO persist(DeviceClassWithEquipmentVO deviceClass);

    DeviceClassWithEquipmentVO merge(DeviceClassWithEquipmentVO deviceClass);

    List<DeviceClassWithEquipmentVO> getDeviceClassList(String name, String namePattern, String sortField,
                                                Boolean sortOrderAsc, Integer take, Integer skip);

    DeviceClassWithEquipmentVO findByName(String name);

    DeviceClassEquipmentVO getByDeviceClassAndId(@NotNull Long deviceClassId, @NotNull long equipmentId);
}
