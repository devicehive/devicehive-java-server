package com.devicehive.auth;

import com.devicehive.model.AvailableActions;

public enum HiveAction {
    GET_NETWORK(AvailableActions.GET_NETWORK),
    GET_DEVICE(AvailableActions.GET_DEVICE),
    GET_DEVICE_STATE(AvailableActions.GET_DEVICE_STATE),
    GET_DEVICE_NOTIFICATION(AvailableActions.GET_DEVICE_NOTIFICATION),
    GET_DEVICE_COMMAND(AvailableActions.GET_DEVICE_COMMAND),
    GET_DEVICE_CLASS(AvailableActions.GET_DEVICE_CLASS),
    REGISTER_DEVICE(AvailableActions.REGISTER_DEVICE),
    CREATE_DEVICE_COMMAND(AvailableActions.CREATE_DEVICE_COMMAND),
    UPDATE_DEVICE_COMMAND(AvailableActions.UPDATE_DEVICE_COMMAND),
    CREATE_DEVICE_NOTIFICATION(AvailableActions.CREATE_DEVICE_NOTIFICATION),

    GET_CURRENT_USER(AvailableActions.GET_CURRENT_USER),
    UPDATE_CURRENT_USER(AvailableActions.UPDATE_CURRENT_USER),
    MANAGE_ACCESS_KEY(AvailableActions.MANAGE_ACCESS_KEY),
    MANAGE_OAUTH_GRANT(AvailableActions.MANAGE_OAUTH_GRANT),

    MANAGE_USER(AvailableActions.MANAGE_USER),
    MANAGE_DEVICE_CLASS(AvailableActions.MANAGE_DEVICE_CLASS),
    MANAGE_NETWORK(AvailableActions.MANAGE_NETWORK),
    MANAGE_OAUTH_CLIENT(AvailableActions.MANAGE_OAUTH_CLIENT),
    MANAGE_TOKEN(AvailableActions.MANAGE_TOKEN),
    NONE(null);

    private String value;

    HiveAction(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static HiveAction fromString(String text) {
        if (text != null) {
            for (HiveAction b : HiveAction.values()) {
                if (text.equalsIgnoreCase(b.value)) {
                    return b;
                }
            }
        }
        return null;
    }
}