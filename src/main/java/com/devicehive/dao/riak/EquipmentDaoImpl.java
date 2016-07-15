package com.devicehive.dao.riak;

import com.devicehive.dao.DeviceClassDao;
import com.devicehive.dao.EquipmentDao;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.*;

@Profile({"riak"})
@Repository
public class EquipmentDaoImpl implements EquipmentDao {

    @Autowired
    private DeviceClassDao deviceClassDao;

    @Override
    public List<Equipment> getByDeviceClass(@NotNull DeviceClass deviceClass) {
        DeviceClass dc = deviceClassDao.find(deviceClass.getId());
        return dc != null ? new ArrayList<>(dc.getEquipment()) : Collections.emptyList();
    }

    @Override
    public Equipment getByDeviceClassAndId(@NotNull String deviceClassName, @NotNull long equipmentId) {
        return find(equipmentId, deviceClassName);
    }

    @Override
    public int deleteByDeviceClass(@NotNull DeviceClass deviceClass) {
        if (deviceClass.getEquipment() != null) {
            int result = deviceClass.getEquipment().size();
            deviceClass.getEquipment().clear();
            deviceClassDao.merge(deviceClass);
            return result;
        }
        return 0;
    }

    @Override
    public boolean deleteByIdAndDeviceClass(@NotNull long equipmentId, @NotNull String deviceClassName) {
        DeviceClass deviceClass = deviceClassDao.find(deviceClassName);
        Equipment stored = null;
        for (Equipment equipment : deviceClass.getEquipment()) {
            if (equipment.getId() == equipmentId) {
                stored = equipment;
            }
        }
        if (stored != null) {
            deviceClass.getEquipment().remove(stored);
            deviceClassDao.merge(deviceClass);
            return true;
        }
        return false;
    }

    @Override
    public void persist(Equipment equipment) {
        merge(equipment);
    }

    @Override
    public Equipment find(long equipmentId, String deviceClassName) {
        DeviceClass deviceClass = deviceClassDao.find(deviceClassName);
        for (Equipment equipment : deviceClass.getEquipment()) {
            if (equipment.getId() == equipmentId) {
                return equipment;
            }
        }
        return null;
    }

    @Override
    public Equipment merge(Equipment equipment) {
        DeviceClass deviceClass = equipment.getDeviceClass();
        //getting it from storage
        deviceClass = deviceClassDao.find(deviceClass.getId());
        if (equipment.getId() == null) {
            // todo: remove id from equipment
            equipment.setId(System.currentTimeMillis());
        }
        if (deviceClass.getEquipment() != null) {
            deviceClass.getEquipment().add(equipment);
        } else {
            Set<Equipment> equipmentSet = new HashSet<>();
            equipmentSet.add(equipment);
            deviceClass.setEquipment(equipmentSet);
        }
        deviceClassDao.merge(deviceClass);

        return equipment;
    }
}
