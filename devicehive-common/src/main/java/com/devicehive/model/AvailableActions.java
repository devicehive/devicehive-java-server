package com.devicehive.model;

/*
 * #%L
 * DeviceHive Java Server Common business logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import com.devicehive.auth.HiveAction;

import java.util.HashSet;
import java.util.Set;

public class AvailableActions {
    private static final String ADMIN_ACTION = "(Admin)";

    public static final String GET_NETWORK = "GetNetwork";
    public static final String GET_DEVICE = "GetDevice";
    public static final String GET_DEVICE_STATE = "GetDeviceState";
    public static final String GET_DEVICE_NOTIFICATION = "GetDeviceNotification";
    public static final String GET_DEVICE_COMMAND = "GetDeviceCommand";
    public static final String GET_DEVICE_CLASS = "GetDeviceClass";
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
    public static final String MANAGE_TOKEN = "ManageToken";

    private static Set<String> CLIENT_ACTIONS = new HashSet<String>() {
        {
            add(GET_NETWORK);
            add(GET_DEVICE);
            add(GET_DEVICE_STATE);
            add(GET_DEVICE_NOTIFICATION);
            add(GET_DEVICE_COMMAND);
            add(GET_DEVICE_CLASS);
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

    private static Set<String> ADMIN_ACTIONS = new HashSet<String>() {
        {
            add(MANAGE_USER);
            add(MANAGE_DEVICE_CLASS);
            add(MANAGE_NETWORK);
            add(MANAGE_OAUTH_CLIENT);
            add(MANAGE_TOKEN);
        }

        private static final long serialVersionUID = -1946208903850253584L;
    };

    private static Set<String> KNOWN_ACTIONS = new HashSet<String>() {
        {
            addAll(CLIENT_ACTIONS);
            addAll(ADMIN_ACTIONS);
        }

        private static final long serialVersionUID = -1571200010251977615L;
    };

    private static Set<HiveAction> CLIENT_HIVE_ACTIONS = new HashSet<>();
    private static Set<HiveAction> KNOWN_HIVE_ACTIONS = new HashSet<>();

    static {
        CLIENT_ACTIONS.forEach(action -> CLIENT_HIVE_ACTIONS.add(HiveAction.fromString(action)));
        KNOWN_ACTIONS.forEach(action -> KNOWN_HIVE_ACTIONS.add(HiveAction.fromString(action)));
    }

    public static boolean validate(Set<String> actions) {
        for (String current : actions) {
            if (!KNOWN_ACTIONS.contains(current)) {
                return false;
            }
        }
        return true;
    }

    public static Set<String> getAllActions() {
        return KNOWN_ACTIONS;
    }

    public static Set<HiveAction> getAllHiveActions() {
        return KNOWN_HIVE_ACTIONS;
    }

    public static Set<HiveAction> getClientHiveActions() {
        return CLIENT_HIVE_ACTIONS;
    }

    public static Set<String> getClientActions() {
        return CLIENT_ACTIONS;
    }

    public static Set<String> getAdminActions() {
        return ADMIN_ACTIONS;
    }

}
