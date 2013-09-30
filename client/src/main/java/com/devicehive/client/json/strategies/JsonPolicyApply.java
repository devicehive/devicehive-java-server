package com.devicehive.client.json.strategies;


import org.glassfish.hk2.api.AnnotationLiteral;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface JsonPolicyApply {

    JsonPolicyDef.Policy value();


}
