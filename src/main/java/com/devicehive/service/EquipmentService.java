package com.devicehive.service;

import com.devicehive.dao.EquipmentDAO;
import com.devicehive.model.Equipment;
import com.devicehive.service.interceptors.ValidationInterceptor;

import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.validation.constraints.NotNull;

/**
 * @author Nikolay Loboda
 * @since 19.07.13
 */
@Singleton
@Interceptors(ValidationInterceptor.class)
public class EquipmentService {

    @Inject
    private EquipmentDAO equipmentDAO;

    public Equipment getEquipmentForDevice(@NotNull long deviceClassId, @NotNull long equipmentId) {
        return equipmentDAO.getByDeviceClass(deviceClassId,equipmentId);
    }

    public Equipment insertEquipment(Equipment e){
        return equipmentDAO.insert(e);
    }

    public Equipment updateEquipment(Equipment e){
        return equipmentDAO.update(e);
    }
    public void deleteEquipment(Equipment e){
        equipmentDAO.delete(e);
    }

    public Equipment get(@NotNull long id){
        return equipmentDAO.get(id);
    }

}
