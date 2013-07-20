package com.devicehive.json.strategies;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface JsonPolicyApply {

    JsonPolicyDef.Policy value();
}
