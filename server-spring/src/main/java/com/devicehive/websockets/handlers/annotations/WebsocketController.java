package com.devicehive.websockets.handlers.annotations;

import javax.inject.Qualifier;
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Qualifier
@Inherited
public @interface WebsocketController {

}
