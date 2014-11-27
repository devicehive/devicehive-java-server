package com.devicehive.model;


import java.util.HashSet;
import java.util.Set;

public class AvailableActions {

    public static final String GET_ACCESS_KEY = "GetAccessKey";
    public static final String CREATE_ACCESS_KEY = "CreateAccessKey";
    public static final String UPDATE_ACCESS_KEY = "UpdateAccessKey";
    public static final String DELETE_ACCESS_KEY = "DeleteAccessKey";
    public static final String GET_DEVICE_CLASS = "GetDeviceClass";
    public static final String CREATE_DEVICE_CLASS = "CreateDeviceClass";
    public static final String UPDATE_DEVICE_CLASS = "UpdateDeviceClass";
    public static final String DELETE_DEVICE_CLASS = "DeleteDeviceClass";
    public static final String GET_NETWORK = "GetNetwork";
    public static final String ASSIGN_NETWORK = "AssignNetwork";
    public static final String CREATE_NETWORK = "CreateNetwork";
    public static final String UPDATE_NETWORK = "UpdateNetwork";
    public static final String DELETE_NETWORK = "DeleteNetwork";
    public static final String CREATE_OAUTH_CLIENT = "CreateOAuthClient";
    public static final String UPDATE_OAUTH_CLIENT = "UpdateOAuthClient";
    public static final String DELETE_OAUTH_CLIENT = "DeleteOAuthClient";
    public static final String GET_OAUTH_GRANT = "GetOAuthGrant";
    public static final String CREATE_OAUTH_GRANT = "CreateOAuthGrant";
    public static final String UPDATE_OAUTH_GRANT = "UpdateOAuthGrant";
    public static final String DELETE_OAUTH_GRANT = "DeleteOAuthGrant";
    public static final String GET_DEVICE = "GetDevice";
    public static final String DELETE_DEVICE = "DeleteDevice";
    public static final String GET_DEVICE_STATE = "GetDeviceState";
    public static final String GET_DEVICE_NOTIFICATION = "GetDeviceNotification";
    public static final String GET_DEVICE_COMMAND = "GetDeviceCommand";
    public static final String REGISTER_DEVICE = "RegisterDevice";
    public static final String CREATE_DEVICE_NOTIFICATION = "CreateDeviceNotification";
    public static final String CREATE_DEVICE_COMMAND = "CreateDeviceCommand";
    public static final String UPDATE_DEVICE_COMMAND = "UpdateDeviceCommand";
    public static final String GET_USER = "GetUser";
    public static final String CREATE_USER = "CreateUser";
    public static final String UPDATE_USER = "UpdateUser";
    public static final String DELETE_USER = "DeleteUser";

    private static Set<String> KNOWN_ACTIONS = new HashSet<String>() {
        {
            add(GET_ACCESS_KEY);
            add(CREATE_ACCESS_KEY);
            add(UPDATE_ACCESS_KEY);
            add(DELETE_ACCESS_KEY);
            add(GET_DEVICE_CLASS);
            add(CREATE_DEVICE_CLASS);
            add(UPDATE_DEVICE_CLASS);
            add(DELETE_DEVICE_CLASS);
            add(GET_NETWORK);
            add(ASSIGN_NETWORK);
            add(CREATE_NETWORK);
            add(UPDATE_NETWORK);
            add(DELETE_NETWORK);
            add(CREATE_OAUTH_CLIENT);
            add(UPDATE_OAUTH_CLIENT);
            add(DELETE_OAUTH_CLIENT);
            add(GET_OAUTH_GRANT);
            add(CREATE_OAUTH_GRANT);
            add(UPDATE_OAUTH_GRANT);
            add(DELETE_OAUTH_GRANT);
            add(GET_DEVICE);
            add(DELETE_DEVICE);
            add(GET_DEVICE_STATE);
            add(GET_DEVICE_NOTIFICATION);
            add(GET_DEVICE_COMMAND);
            add(REGISTER_DEVICE);
            add(CREATE_DEVICE_NOTIFICATION);
            add(CREATE_DEVICE_COMMAND);
            add(UPDATE_DEVICE_COMMAND);
            add(GET_USER);
            add(CREATE_USER);
            add(UPDATE_USER);
            add(DELETE_USER);
        }

        private static final long serialVersionUID = -6981208010851957614L;
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

}
