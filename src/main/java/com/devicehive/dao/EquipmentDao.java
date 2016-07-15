package com.devicehive.dao;

import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Created by Gleb on 07.07.2016.
 */
public interface EquipmentDao {

    List<Equipment> getByDeviceClass(@NotNull DeviceClass deviceClass);

    Equipment getByDeviceClassAndId(@NotNull Long deviceClassId, @NotNull long equipmentId);

    int deleteByDeviceClass(@NotNull DeviceClass deviceClass);

    boolean deleteByIdAndDeviceClass(@NotNull long equipmentId, @NotNull Long deviceClassId);

    void persist(Equipment equipment);

    Equipment find(long equipmentId, Long deviceClassId);

    Equipment merge(Equipment equipment);
}
