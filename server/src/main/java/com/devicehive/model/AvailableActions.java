package com.devicehive.model;


import java.util.HashSet;
import java.util.Set;

public class AvailableActions {
    public static final String GET_NETWORK = "GetNetwork";
    public static final String GET_DEVICE = "GetDevice";
    public static final String GET_DEVICE_STATE = "GetDeviceState";
    public static final String GET_DEVICE_NOTIFICATION = "GetDeviceNotification";
    public static final String GET_DEVICE_COMMAND = "GetDeviceCommand";
    public static final String REGISTER_DEVICE = "RegisterDevice";
    public static final String CREATE_DEVICE_NOTIFICATION = "CreateDeviceNotification";
    public static final String CREATE_DEVICE_COMMAND = "CreateDeviceCommand";
    public static final String UPDATE_DEVICE_COMMAND = "UpdateDeviceCommand";

    private static Set KNOWN_ACTIONS = new HashSet() {{
        add(GET_NETWORK.toUpperCase());
        add(GET_DEVICE.toUpperCase());
        add(GET_DEVICE_STATE.toUpperCase());
        add(GET_DEVICE_NOTIFICATION.toUpperCase());
        add(GET_DEVICE_COMMAND.toUpperCase());
        add(REGISTER_DEVICE.toUpperCase());
        add(CREATE_DEVICE_NOTIFICATION.toUpperCase());
        add(CREATE_DEVICE_COMMAND.toUpperCase());
        add(UPDATE_DEVICE_COMMAND.toUpperCase());
    }

        private static final long serialVersionUID = -6981208010851957614L;
    };

    public static boolean isAvailable(String action){
        String actionUpper = action.toUpperCase();
        return KNOWN_ACTIONS.contains(actionUpper);
    }

    public static boolean validate(Set<String> actions){
        for(String current : actions){
            String actionUpper = current.toUpperCase();
            if (!KNOWN_ACTIONS.contains(actionUpper)){
                return false;
            }
        }
        return true;
    }

}
