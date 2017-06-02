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

import com.devicehive.model.AvailableActions;

public enum HiveAction {
    GET_NETWORK(AvailableActions.GET_NETWORK),
    GET_DEVICE(AvailableActions.GET_DEVICE),
    GET_DEVICE_STATE(AvailableActions.GET_DEVICE_STATE),
    GET_DEVICE_NOTIFICATION(AvailableActions.GET_DEVICE_NOTIFICATION),
    GET_DEVICE_COMMAND(AvailableActions.GET_DEVICE_COMMAND),
    REGISTER_DEVICE(AvailableActions.REGISTER_DEVICE),
    CREATE_DEVICE_COMMAND(AvailableActions.CREATE_DEVICE_COMMAND),
    UPDATE_DEVICE_COMMAND(AvailableActions.UPDATE_DEVICE_COMMAND),
    CREATE_DEVICE_NOTIFICATION(AvailableActions.CREATE_DEVICE_NOTIFICATION),

    GET_CURRENT_USER(AvailableActions.GET_CURRENT_USER),
    UPDATE_CURRENT_USER(AvailableActions.UPDATE_CURRENT_USER),
    MANAGE_ACCESS_KEY(AvailableActions.MANAGE_ACCESS_KEY),
    MANAGE_OAUTH_GRANT(AvailableActions.MANAGE_OAUTH_GRANT),

    MANAGE_USER(AvailableActions.MANAGE_USER),
    MANAGE_CONFIGURATION(AvailableActions.MANAGE_CONFIGURATION),
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
