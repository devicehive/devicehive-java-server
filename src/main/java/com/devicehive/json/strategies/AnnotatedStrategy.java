package com.devicehive.json.strategies;


import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

import javax.validation.constraints.NotNull;

public class AnnotatedStrategy implements ExclusionStrategy {

    private final JsonPolicyDef.Policy policy;

    public AnnotatedStrategy(@NotNull JsonPolicyDef.Policy policy) {
        this.policy = policy;
    }

    @Override
    public boolean shouldSkipField(FieldAttributes f) {
        JsonPolicyDef hjp = f.getAnnotation(JsonPolicyDef.class);
        if (hjp == null) {
            return true;
        }
        for (JsonPolicyDef.Policy definedPolicy : hjp.value()) {
            if (definedPolicy == policy) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
        return false;
    }
}
