package com.devicehive.service;

import com.devicehive.exceptions.HiveException;
import com.devicehive.model.Equipment;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.HashSet;
import java.util.Set;


public class EquipmentService {

    public boolean validateEquipments(Set<Equipment> equipmentSet){
        ValidatorFactory vf = Validation.buildDefaultValidatorFactory();
        Validator validator = vf.getValidator();
        Set<String> validationErrorsSet = new HashSet<>();
        for(Equipment equipment:equipmentSet){
             validationErrorsSet.addAll(Equipment.validate(equipment, validator));
        }
        if (!validationErrorsSet.isEmpty()){
            String exceptionMessage = "Validation errors in equipment: ";
            StringBuilder builder = new StringBuilder();
            for (String violation: validationErrorsSet){
                builder.append(violation);
            }
            exceptionMessage += builder.toString();
            throw new HiveException(exceptionMessage);
        }
        return true;
    }

}
