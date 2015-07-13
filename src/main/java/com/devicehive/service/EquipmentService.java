package com.devicehive.service;

import com.devicehive.dao.CacheConfig;
import com.devicehive.dao.GenericDAO;
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
import java.util.Optional;

/**
 * This class manages equipment in database. EquipmentDAO shouldn't be used directly from controller, please use this
 * class instead
 */
@Component
@Transactional(propagation = Propagation.SUPPORTS)
public class EquipmentService {

    @Autowired
    private GenericDAO genericDAO;
    @Autowired
    private HiveValidator validationUtil;

    /**
     * Delete Equipment (not DeviceEquipment, but whole equipment with appropriate device Equipments)
     *
     * @param equipmentId   equipment id to delete
     * @param deviceClassId id of deviceClass which equipment belongs used to double check
     * @return true if deleted successfully
     */
    @Transactional
    public boolean delete(@NotNull long equipmentId, @NotNull long deviceClassId) {
        return genericDAO.createNamedQuery("Equipment.deleteByIdAndDeviceClass", Optional.<CacheConfig>empty())
                .setParameter("id", equipmentId)
                .setParameter("deviceClassId", deviceClassId)
                .executeUpdate() != 0;
    }

    @Transactional
    public Equipment create(Equipment equipment) {
        genericDAO.persist(equipment);
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
        return genericDAO.createNamedQuery(Equipment.class, "Equipment.getByDeviceClassAndId", Optional.of(CacheConfig.get()))
                .setParameter("id", equipmentId)
                .setParameter("deviceClassId", deviceClassId)
                .getResultList()
                .stream().findFirst().orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Equipment> getByDeviceClass(@NotNull DeviceClass deviceClass) {
        return genericDAO.createNamedQuery(Equipment.class, "Equipment.getByDeviceClass", Optional.of(CacheConfig.get()))
                .setParameter("deviceClass", deviceClass)
                .getResultList();
    }

    @Transactional
    public int deleteByDeviceClass(@NotNull DeviceClass deviceClass) {
        return genericDAO.createNamedQuery("Equipment.deleteByDeviceClass", Optional.<CacheConfig>empty())
                .setParameter("deviceClass", deviceClass)
                .executeUpdate();
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
        Equipment stored = genericDAO.find(Equipment.class, equipmentId);
        if (stored == null || stored.getDeviceClass().getId() != deviceClassId) {
            return false; // equipment with id = equipmentId does not exists
        }
        if (equipmentUpdate.getCode() != null) {
            stored.setCode(equipmentUpdate.getCode().getValue());
        }
        if (equipmentUpdate.getName() != null) {
            stored.setName(equipmentUpdate.getName().getValue());
        }
        if (equipmentUpdate.getType() != null) {
            stored.setType(equipmentUpdate.getType().getValue());
        }
        if (equipmentUpdate.getData() != null) {
            stored.setData(equipmentUpdate.getData().getValue());
        }
        validationUtil.validate(stored);
        genericDAO.merge(stored);
        return true;
    }
}
