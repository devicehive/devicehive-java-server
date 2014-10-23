package com.devicehive.auth;


import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

/**
 * Used for as a marker for WWW_AUTHENTICATE header adding.
 */
@Target({METHOD, TYPE})
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface WwwAuthenticateRequired {

}
