package com.devicehive.auth;

import com.devicehive.model.AvailableActions;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

@Target({METHOD, TYPE})
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedKeyAction {

    AccessKeyAction action() default AccessKeyAction.NONE;

}
