package com.devicehive.json.strategies;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface JsonPolicyDef {

    Policy[] value();

    public static enum Policy {
        WEBSOCKET_SERVER_INFO,
        REST_SERVER_INFO,
        DEVICE_PUBLISHED,
        DEVICE_SUBMITTED,
        COMMAND_TO_CLIENT,
        COMMAND_TO_DEVICE,
        COMMAND_LISTED,
        POST_COMMAND_TO_DEVICE,
        COMMAND_FROM_CLIENT,
        COMMAND_UPDATE_FROM_DEVICE,
        COMMAND_UPDATE_TO_CLIENT,
        NOTIFICATION_FROM_DEVICE,
        NOTIFICATION_TO_DEVICE,
        NOTIFICATION_TO_CLIENT,
        DEVICE_EQUIPMENT_SUBMITTED,
        EQUIPMENT_SUBMITTED,
        USER_PUBLISHED,
        USERS_LISTED,
        NETWORK_PUBLISHED,
        NETWORKS_LISTED,
        NETWORK_SUBMITTED,
        DEVICECLASS_LISTED,
        DEVICECLASS_PUBLISHED,
        DEVICECLASS_SUBMITTED,
        EQUIPMENTCLASS_PUBLISHED,
        EQUIPMENTCLASS_SUBMITTED,
        USER_SUBMITTED,
        WS_COMMAND_TO_DEVICE,
        WS_NOTIFICATION_TO_DEVICE
    }
}
