package com.devicehive.auth;

import com.devicehive.model.AvailableActions;

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
public @interface AllowedKeyAction {

    @Nonbinding
    public Action[] action() default Action.NONE;

    public static enum Action{
        GET_NETWORK(AvailableActions.GET_NETWORK),
        GET_DEVICE(AvailableActions.GET_DEVICE),
        GET_DEVICE_STATE(AvailableActions.GET_DEVICE_STATE),
        GET_DEVICE_NOTIFICATION(AvailableActions.GET_DEVICE_NOTIFICATION),
        GET_DEVICE_COMMAND(AvailableActions.GET_DEVICE_COMMAND),
        REGISTER_DEVICE(AvailableActions.REGISTER_DEVICE),
        CREATE_DEVICE_COMMAND(AvailableActions.CREATE_DEVICE_COMMAND),
        UPDATE_DEVICE_COMMAND(AvailableActions.UPDATE_DEVICE_COMMAND),
        CREATE_DEVICE_NOTIFICATION(AvailableActions.CREATE_DEVICE_NOTIFICATION),
        NONE(null);

        private String value;

        private Action(String value){
            this.value = value;
        }

        public String getValue(){
            return value;
        }
    }
}
