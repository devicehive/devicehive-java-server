package com.devicehive.auth;

import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

@Target({METHOD, TYPE})
@Inherited
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedAction {

    @Nonbinding
    public Action[] action() default Action.NONE;

    public static enum Action{
        GET_NETWORK,
        GET_DEVICE,
        GET_DEVICE_STATE,
        GET_DEVICE_NOTIFICATION,
        GET_DEVICE_COMMAND,
        REGISTER_DEVICE,
        CREATE_DEVICE_COMMAND,
        UPDATE_DEVICE_COMMAND,
        CREATE_DEVICE_NOTIFICATION,
        NONE
    }
}
