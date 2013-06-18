package com.devicehive.websockets.json.strategies;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: jkulagina
 * Date: 18.06.13
 * Time: 14:54
 */
public class CommandInsertResponseExclusionStrategy implements ExclusionStrategy {
    private static final Set<String> fieldNamesToExclude;

    static{
        Set<String> initSet = new HashSet<>();
        initSet.add("device");
        initSet.add("command");
        initSet.add("parameters");
        initSet.add("lifetime");
        initSet.add("flags");
        initSet.add("status");
        initSet.add("result");
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
}
