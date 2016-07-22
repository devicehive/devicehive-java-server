package com.devicehive.service;

import com.devicehive.dao.EquipmentDao;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import com.devicehive.model.updates.EquipmentUpdate;
import com.devicehive.util.HiveValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * This class manages equipment in database. EquipmentDAO shouldn't be used directly from controller, please use this
 * class instead
 */
@Component
@Transactional(propagation = Propagation.SUPPORTS)
public class EquipmentService {

    @Autowired
    private HiveValidator validationUtil;
    @Autowired
    private EquipmentDao equipmentDao;

    /**
     * Delete Equipment (not DeviceEquipment, but whole equipment with appropriate device Equipments)
     *
     * @param equipmentId   equipment id to delete
     * @param deviceClassId id of deviceClass which equipment belongs used to double check
     * @return true if deleted successfully
     */
    @Transactional
    public boolean delete(@NotNull long equipmentId, @NotNull long deviceClassId) {
        return equipmentDao.deleteByIdAndDeviceClass(equipmentId, deviceClassId);
    }

    @Transactional
    public Equipment create(Equipment equipment) {
        equipmentDao.persist(equipment);
        return equipment;
    }

    /**
     * Retrieves Equipment from database
     *
     * @param deviceClassId parent device class id for this equipment
     * @param equipmentId   id of equipment to get
     */
    @Transactional(readOnly = true)
    public Equipment getByDeviceClass(@NotNull long deviceClassId, @NotNull long equipmentId) {
        return equipmentDao.getByDeviceClassAndId(deviceClassId, equipmentId);
    }

    @Transactional(readOnly = true)
    public List<Equipment> getByDeviceClass(@NotNull DeviceClass deviceClass) {
        return equipmentDao.getByDeviceClass(deviceClass);
    }

    @Transactional
    public int deleteByDeviceClass(@NotNull DeviceClass deviceClass) {
        return equipmentDao.deleteByDeviceClass(deviceClass);
    }

    /**
     * updates Equipment attributes
     *
     * @param equipmentUpdate Equipment instance, containing fields to update (Id field will be ignored)
     * @param equipmentId     id of equipment to update
     * @param deviceClassId   class of equipment to update
     * @return true, if update successful, false otherwise
     */
    @Transactional
    public boolean update(EquipmentUpdate equipmentUpdate, @NotNull long equipmentId, @NotNull long deviceClassId) {
        if (equipmentUpdate == null) {
            return true;
        }
        Equipment stored = equipmentDao.find(equipmentId, deviceClassId);
        if (stored == null || stored.getDeviceClass().getId() != deviceClassId) {
            return false; // equipment with id = equipmentId does not exists
        }
        if (equipmentUpdate.getCode() != null) {
            stored.setCode(equipmentUpdate.getCode().orElse(null));
        }
        if (equipmentUpdate.getName() != null) {
            stored.setName(equipmentUpdate.getName().orElse(null));
        }
        if (equipmentUpdate.getType() != null) {
            stored.setType(equipmentUpdate.getType().orElse(null));
        }
        if (equipmentUpdate.getData() != null) {
            stored.setData(equipmentUpdate.getData().orElse(null));
        }
        validationUtil.validate(stored);
        equipmentDao.merge(stored);
        return true;
    }
}
