package com.devicehive.websockets.handlers;

import com.devicehive.model.AuthLevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Action {

    public String value();


    public AuthLevel requredLevel() default AuthLevel.NONE;

    public boolean copyRequestId() default false;
}
