package com.devicehive.json.strategies;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface JsonPolicyDef {

    Policy[] value();

    public static enum Policy {
        WEBSOCKET_SERVER_INFO,
        REST_SERVER_INFO
    }
}
