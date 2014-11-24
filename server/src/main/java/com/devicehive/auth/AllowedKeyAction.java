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
    public Action action() default Action.NONE;

    public static enum Action {
        GET_ACCESS_KEY(AvailableActions.GET_ACCESS_KEY),
        CREATE_ACCESS_KEY(AvailableActions.CREATE_ACCESS_KEY),
        UPDATE_ACCESS_KEY(AvailableActions.UPDATE_ACCESS_KEY),
        DELETE_ACCESS_KEY(AvailableActions.DELETE_ACCESS_KEY),
        GET_DEVICE_CLASS(AvailableActions.GET_DEVICE_CLASS),
        CREATE_DEVICE_CLASS(AvailableActions.CREATE_DEVICE_CLASS),
        UPDATE_DEVICE_CLASS(AvailableActions.UPDATE_DEVICE_CLASS),
        DELETE_DEVICE_CLASS(AvailableActions.DELETE_DEVICE_CLASS),
        GET_NETWORK(AvailableActions.GET_NETWORK),
        ASSIGN_NETWORK(AvailableActions.ASSIGN_NETWORK),
        CREATE_NETWORK(AvailableActions.CREATE_NETWORK),
        UPDATE_NETWORK(AvailableActions.UPDATE_NETWORK),
        DELETE_NETWORK(AvailableActions.DELETE_NETWORK),
        CREATE_OAUTH_CLIENT(AvailableActions.CREATE_OAUTH_CLIENT),
        UPDATE_OAUTH_CLIENT(AvailableActions.UPDATE_OAUTH_CLIENT),
        DELETE_OAUTH_CLIENT(AvailableActions.DELETE_OAUTH_CLIENT),
        GET_OAUTH_GRANT(AvailableActions.GET_OAUTH_GRANT),
        CREATE_OAUTH_GRANT(AvailableActions.CREATE_OAUTH_GRANT),
        UPDATE_OAUTH_GRANT(AvailableActions.UPDATE_OAUTH_GRANT),
        DELETE_OAUTH_GRANT(AvailableActions.DELETE_OAUTH_GRANT),
        GET_DEVICE(AvailableActions.GET_DEVICE),
        DELETE_DEVICE(AvailableActions.DELETE_DEVICE),
        GET_DEVICE_STATE(AvailableActions.GET_DEVICE_STATE),
        GET_DEVICE_NOTIFICATION(AvailableActions.GET_DEVICE_NOTIFICATION),
        GET_DEVICE_COMMAND(AvailableActions.GET_DEVICE_COMMAND),
        REGISTER_DEVICE(AvailableActions.REGISTER_DEVICE),
        CREATE_DEVICE_COMMAND(AvailableActions.CREATE_DEVICE_COMMAND),
        UPDATE_DEVICE_COMMAND(AvailableActions.UPDATE_DEVICE_COMMAND),
        CREATE_DEVICE_NOTIFICATION(AvailableActions.CREATE_DEVICE_NOTIFICATION),
        GET_USER(AvailableActions.GET_USER),
        CREATE_USER(AvailableActions.CREATE_USER),
        UPDATE_USER(AvailableActions.UPDATE_USER),
        DELETE_USER(AvailableActions.DELETE_USER),
        NONE(null);

        private String value;

        private Action(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
