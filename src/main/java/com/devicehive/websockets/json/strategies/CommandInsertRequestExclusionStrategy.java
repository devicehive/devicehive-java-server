package com.devicehive.websockets.json.strategies;

import com.devicehive.model.DeviceCommand;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: jkulagina
 * Date: 18.06.13
 * Time: 14:26
 */
public class CommandInsertRequestExclusionStrategy implements ExclusionStrategy{
    private static final Set<String> fieldNamesToExclude;

    static{
        Set<String> initSet = new HashSet<>();
        initSet.add("id");
        initSet.add("timestamp");
        initSet.add("user");
        initSet.add("device");
        fieldNamesToExclude = initSet;
    }

    @Override
    public boolean shouldSkipField(FieldAttributes fieldAttributes) {
        return fieldNamesToExclude.contains(fieldAttributes.getName());
    }

    @Override
    public boolean shouldSkipClass(Class<?> aClass) {
        return false;
    }

    public static void main(String ... args){
        Field[] fields = DeviceCommand.class.getFields();
        for(Field field: fields){
            FieldAttributes attributes = new FieldAttributes(field);
            System.out.println(attributes.getName() + " ");
        }
    }
}
