package com.devicehive.service;

import com.devicehive.dao.EquipmentDAO;
import com.devicehive.model.Equipment;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

/**
 * This class manages equipment in database. EquipmentDAO shouldn't
 * be used directly from controller, please use this class instead
 *
 * @author Nikolay Loboda
 * @since 06.08.2013
 */
@Stateless
public class EquipmentService {

    @Inject
    private EquipmentDAO equipmentDAO;

    /**
     * Delete Equipment (not DeviceEquipment, but whole equipment with appropriate device Equipments)
     *
     * @param equipmentId   equipment id to delete
     * @param deviceClassId id of deviceClass which equipment belongs used to double check
     * @return true if deleted successfully
     */
    public boolean delete(@NotNull long equipmentId, @NotNull long deviceClassId) {
        return equipmentDAO.delete(equipmentId, deviceClassId);
    }

    /**
     * Retrieves Equipment from database
     *
     * @param deviceClassId parent device class id for this equipment
     * @param equipmentId   id of equipment to get
     * @return
     */
    public Equipment getByDeviceClass(@NotNull long deviceClassId, @NotNull long equipmentId) {
        return equipmentDAO.getByDeviceClass(deviceClassId, equipmentId);
    }


    /**
     * updates Equipment attributes
     *
     * @param equipment     Equipment instance, containing fields to update (Id field will be ignored)
     * @param equipmentId   id of equipment to update
     * @param deviceClassId class of equipment to update
     * @return true, if update successful, false otherwise
     */
    public boolean update(@NotNull Equipment equipment, @NotNull long equipmentId, @NotNull long deviceClassId) {
        return equipmentDAO.update(equipment, equipmentId, deviceClassId);
    }
}
