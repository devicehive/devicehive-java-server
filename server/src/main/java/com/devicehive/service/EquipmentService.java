package com.devicehive.service;

import com.devicehive.dao.EquipmentDAO;
import com.devicehive.model.Equipment;
import com.devicehive.model.updates.EquipmentUpdate;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
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

    @EJB
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
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Equipment getByDeviceClass(@NotNull long deviceClassId, @NotNull long equipmentId) {
        return equipmentDAO.getByDeviceClass(deviceClassId, equipmentId);
    }


    /**
     * updates Equipment attributes
     *
     * @param equipmentUpdate Equipment instance, containing fields to update (Id field will be ignored)
     * @param equipmentId     id of equipment to update
     * @param deviceClassId   class of equipment to update
     * @return true, if update successful, false otherwise
     */
    public boolean update(EquipmentUpdate equipmentUpdate, @NotNull long equipmentId, @NotNull long deviceClassId) {
        if (equipmentUpdate == null) {
            return true;
        }
        Equipment stored = equipmentDAO.get(equipmentId);
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
        return true;
    }
}
