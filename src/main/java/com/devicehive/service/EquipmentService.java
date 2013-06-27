package com.devicehive.service;

import com.devicehive.dao.EquipmentDAO;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Set;

public class EquipmentService {

    @Inject
    private EquipmentDAO equipmentDAO;

        @Transactional
    public void saveOrUpdateEquipments(Set<Equipment> equipmentSet){
        for (Equipment equipment: equipmentSet){
            Equipment findByCodeEquipment = equipmentDAO.findByCode(equipment.getCode());
            if (findByCodeEquipment == null){
                equipmentDAO.saveEquipment(equipment);
            }
            else{
                equipment.setId(findByCodeEquipment.getId());
                equipmentDAO.updateEquipment(equipment);
            }
        }
    }


    @Transactional
    public void removeUnusefulEquipments(DeviceClass deviceClass, Set<Equipment> equipmentSet){
        List<Equipment> existingEquipments =  equipmentDAO.getByDeviceClass(deviceClass);
        for (Equipment existingEquipment: existingEquipments){
            boolean shouldRemove = true;
            for (Equipment newEquipment: equipmentSet){
                if (newEquipment.getCode().equals(existingEquipment.getCode())){
                    shouldRemove = false;
                }
            }
            if (shouldRemove){
//                em.remove(existingEquipment);
                equipmentDAO.removeEquipment(existingEquipment);
            }
        }
    }

//    public boolean validateEquipments(Set<Equipment> equipmentSet){
//        ValidatorFactory vf = Validation.buildDefaultValidatorFactory();
//        Validator validator = vf.getValidator();
//        Set<String> validationErrorsSet = new HashSet<>();
//        for(Equipment equipment:equipmentSet){
//             validationErrorsSet.addAll(Equipment.validate(equipment, validator));
//        }
//        if (!validationErrorsSet.isEmpty()){
//            String exceptionMessage = "Validation errors in equipment: ";
//            StringBuilder builder = new StringBuilder();
//            for (String violation: validationErrorsSet){
//                builder.append(violation);
//            }
//            exceptionMessage += builder.toString();
//            throw new HiveException(exceptionMessage);
//        }
//        return true;
//    }

}
