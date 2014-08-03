package com.devicehive.auth;

import javax.interceptor.InterceptorBinding;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;


@Target({METHOD, TYPE})
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Authorized {
}
