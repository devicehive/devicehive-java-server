package com.devicehive.model;


import java.util.HashSet;
import java.util.Set;

public class AvailableActions {
    private static final String ADMIN_ACTION = "(Admin)";

    public static final String GET_NETWORK = "GetNetwork";
    public static final String GET_DEVICE = "GetDevice";
    public static final String GET_DEVICE_STATE = "GetDeviceState";
    public static final String GET_DEVICE_NOTIFICATION = "GetDeviceNotification";
    public static final String GET_DEVICE_COMMAND = "GetDeviceCommand";
    public static final String REGISTER_DEVICE = "RegisterDevice";
    public static final String CREATE_DEVICE_NOTIFICATION = "CreateDeviceNotification";
    public static final String CREATE_DEVICE_COMMAND = "CreateDeviceCommand";
    public static final String UPDATE_DEVICE_COMMAND = "UpdateDeviceCommand";

    public static final String GET_CURRENT_USER = "GetCurrentUser";
    public static final String UPDATE_CURRENT_USER = "UpdateCurrentUser";
    public static final String MANAGE_ACCESS_KEY = "ManageAccessKey";
    public static final String MANAGE_OAUTH_GRANT = "ManageOAuthGrant";

    //admin
    public static final String MANAGE_USER = "ManageUser";
    public static final String MANAGE_DEVICE_CLASS = "ManageDeviceClass";
    public static final String MANAGE_NETWORK = "ManageNetwork";
    public static final String MANAGE_OAUTH_CLIENT = "ManageOAuthClient";

    private static Set<String> CLIENT_ACTIONS = new HashSet<String>() {
        {
            add(GET_NETWORK);
            add(GET_DEVICE);
            add(GET_DEVICE_STATE);
            add(GET_DEVICE_NOTIFICATION);
            add(GET_DEVICE_COMMAND);
            add(REGISTER_DEVICE);
            add(CREATE_DEVICE_NOTIFICATION);
            add(CREATE_DEVICE_COMMAND);
            add(UPDATE_DEVICE_COMMAND);

            add(GET_CURRENT_USER);
            add(UPDATE_CURRENT_USER);
            add(MANAGE_ACCESS_KEY);
            add(MANAGE_OAUTH_GRANT);
        }

        private static final long serialVersionUID = -6981208010851957614L;
    };

    private static Set<String> KNOWN_ACTIONS = new HashSet<String>() {
        {
            addAll(CLIENT_ACTIONS);

            add(MANAGE_USER);
            add(MANAGE_DEVICE_CLASS);
            add(MANAGE_NETWORK);
            add(MANAGE_OAUTH_CLIENT);
        }

        private static final long serialVersionUID = -1571200010251977615L;
    };

    public static boolean validate(Set<String> actions) {
        for (String current : actions) {
            if (!KNOWN_ACTIONS.contains(current)) {
                return false;
            }
        }
        return true;
    }

    public static String[] getAllActions() {
        return KNOWN_ACTIONS.toArray(new String[KNOWN_ACTIONS.size()]);
    }

    public static String[] getClientActions() {
        return CLIENT_ACTIONS.toArray(new String[CLIENT_ACTIONS.size()]);
    }

}
