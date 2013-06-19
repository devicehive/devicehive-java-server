package com.devicehive.websockets.json.strategies;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: jkulagina
 * Date: 19.06.13
 * Time: 16:30
 */
public class CommandInsertResponseExclusionStrategy implements ExclusionStrategy {
    private static final Set<String> FIELD_NAMES_TO_EXCLUDE;

    static{
        Set<String> initSet = new HashSet<>();
        initSet.add("command");
        initSet.add("parameters");
        initSet.add("lifetime");
        initSet.add("flags");
        initSet.add("status");
        initSet.add("result");
        FIELD_NAMES_TO_EXCLUDE = initSet;
    }

    @Override
    public boolean shouldSkipField(FieldAttributes fieldAttributes) {
        return FIELD_NAMES_TO_EXCLUDE.contains(fieldAttributes.getName());
    }

    @Override
    public boolean shouldSkipClass(Class<?> aClass) {
        return false;
    }
}
