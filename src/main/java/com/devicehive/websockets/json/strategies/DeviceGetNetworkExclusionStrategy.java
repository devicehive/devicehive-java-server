package com.devicehive.websockets.json.strategies;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

import java.util.HashSet;
import java.util.Set;


public class DeviceGetNetworkExclusionStrategy implements ExclusionStrategy {
    private static final Set<String> FIELD_NAMES_TO_INCLUDE = new HashSet<String>(){
        {
            add("id");
            add("name");
            add("description");
        }
    };

    @Override
    public boolean shouldSkipField(FieldAttributes fieldAttributes) {
        return !FIELD_NAMES_TO_INCLUDE.contains(fieldAttributes.getName());
    }

    @Override
    public boolean shouldSkipClass(Class<?> aClass) {
        return false;
    }
}