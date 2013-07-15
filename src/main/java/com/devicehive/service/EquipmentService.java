package com.devicehive.service;

import com.devicehive.dao.EquipmentDAO;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import com.devicehive.service.interceptors.ValidationInterceptor;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Set;

@Interceptors(ValidationInterceptor.class)
@Stateless
public class EquipmentService {

    @Inject
    private EquipmentDAO equipmentDAO;

    @Transactional
    public void saveOrUpdateEquipments(Set<Equipment> equipmentSet) {
        for (Equipment equipment : equipmentSet) {
            Equipment findByCodeEquipment = equipmentDAO.findByCode(equipment.getCode());
            if (findByCodeEquipment == null) {
                equipmentDAO.saveEquipment(equipment);
            } else {
                findByCodeEquipment.setDeviceClass(equipment.getDeviceClass());
                findByCodeEquipment.setData(equipment.getData());
                findByCodeEquipment.setName(equipment.getName());
                findByCodeEquipment.setCode(equipment.getCode());
                findByCodeEquipment.setType(equipment.getType());
                equipmentDAO.updateEquipment(findByCodeEquipment);
            }
        }
    }

    @Transactional
    public void removeUnusefulEquipments(DeviceClass deviceClass, Set<Equipment> equipmentSet) {
        List<Equipment> existingEquipments = equipmentDAO.getByDeviceClass(deviceClass);
        for (Equipment existingEquipment : existingEquipments) {
            boolean shouldRemove = true;
            for (Equipment newEquipment : equipmentSet) {
                if (newEquipment.getCode().equals(existingEquipment.getCode())) {
                    shouldRemove = false;
                }
            }
            if (shouldRemove) {
                equipmentDAO.removeEquipment(existingEquipment);
            }
        }
    }


}
