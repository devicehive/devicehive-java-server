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

    Equipment getByDeviceClassAndId(@NotNull String deviceClassName, @NotNull long equipmentId);

    int deleteByDeviceClass(@NotNull DeviceClass deviceClass);

    boolean deleteByIdAndDeviceClass(@NotNull long equipmentId, @NotNull String deviceClassName);

    void persist(Equipment equipment);

    Equipment find(long equipmentId, String deviceClassName);

    Equipment merge(Equipment equipment);
}
