package com.devicehive.auth;

/*
 * #%L
 * DeviceHive Common Module
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

import com.devicehive.configuration.Constants;
import com.google.common.collect.ImmutableSet;

import java.util.Set;
import java.util.stream.Collectors;

public enum HiveAction {
    ANY(0, Constants.ANY),
    NONE(1, null),
    GET_NETWORK(2, Constants.GET_NETWORK),
    GET_DEVICE(3,Constants.GET_DEVICE),
    GET_DEVICE_NOTIFICATION(4, Constants.GET_DEVICE_NOTIFICATION),
    GET_DEVICE_COMMAND(5, Constants.GET_DEVICE_COMMAND),
    REGISTER_DEVICE(6, Constants.REGISTER_DEVICE),
    CREATE_DEVICE_COMMAND(7, Constants.CREATE_DEVICE_COMMAND),
    UPDATE_DEVICE_COMMAND(8, Constants.UPDATE_DEVICE_COMMAND),
    CREATE_DEVICE_NOTIFICATION(9, Constants.CREATE_DEVICE_NOTIFICATION),

    GET_CURRENT_USER(10, Constants.GET_CURRENT_USER),
    UPDATE_CURRENT_USER(11, Constants.UPDATE_CURRENT_USER),

    MANAGE_USER(12, Constants.MANAGE_USER),
    MANAGE_CONFIGURATION(13, Constants.MANAGE_CONFIGURATION),
    MANAGE_NETWORK(14, Constants.MANAGE_NETWORK),
    MANAGE_TOKEN(15, Constants.MANAGE_TOKEN),
    MANAGE_PLUGIN(16, Constants.MANAGE_PLUGIN),

    GET_DEVICE_TYPE(17, Constants.GET_DEVICE_TYPE),
    MANAGE_DEVICE_TYPE(18, Constants.MANAGE_DEVICE_TYPE);

    private static Set<HiveAction> CLIENT_ACTIONS = ImmutableSet.of(GET_NETWORK, GET_DEVICE_TYPE, GET_DEVICE, GET_DEVICE_NOTIFICATION,
            GET_DEVICE_COMMAND, REGISTER_DEVICE, CREATE_DEVICE_NOTIFICATION, CREATE_DEVICE_COMMAND,
            UPDATE_DEVICE_COMMAND, GET_CURRENT_USER, UPDATE_CURRENT_USER, MANAGE_TOKEN, MANAGE_PLUGIN);

    private static Set<HiveAction> ADMIN_ACTIONS = ImmutableSet.of(MANAGE_USER, MANAGE_CONFIGURATION, MANAGE_NETWORK,
            MANAGE_DEVICE_TYPE);

    private static Set<HiveAction> KNOWN_ACTIONS = ImmutableSet.<HiveAction>builder()
            .addAll(CLIENT_ACTIONS)
            .addAll(ADMIN_ACTIONS)
            .build();

    private Integer id;
    private String value;

    HiveAction(Integer id, String value) {
        this.id = id;
        this.value = value;
    }

    public Integer getId() {
        return id;
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

    public static HiveAction fromId(Integer id) {
        if (id != null) {
            for (HiveAction b : HiveAction.values()) {
                if (id.equals(b.id)) {
                    return b;
                }
            }
        }
        return null;
    }

    public static Set<HiveAction> getAllHiveActions() {
        return KNOWN_ACTIONS;
    }

    public static Set<HiveAction> getClientHiveActions() {
        return CLIENT_ACTIONS;
    }

    public static Set<HiveAction> getAdminHiveActions() {
        return ADMIN_ACTIONS;
    }

    public static Set<Integer> getIdSet(Set<HiveAction> actions) {
        return actions.stream()
                .mapToInt(hiveAction -> hiveAction.getId())
                .boxed()
                .collect(Collectors.toSet());
    }

    public static Set<HiveAction> getActionSet(Set<Integer> ids) {
        HiveAction[] actions = values();
        return ids.stream()
                .map(id -> actions[id])
                .collect(Collectors.toSet());
    }    
    
}
