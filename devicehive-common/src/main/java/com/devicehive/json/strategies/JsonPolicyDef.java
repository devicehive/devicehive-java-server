package com.devicehive.json.strategies;

/*
 * #%L
 * DeviceHive Common Dao Interfaces
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


import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface JsonPolicyDef {

    Policy[] value();

    public static enum Policy {
        JWT_ACCESS_TOKEN_SUBMITTED,
        JWT_REFRESH_TOKEN_SUBMITTED,
        WEBSOCKET_SERVER_INFO,
        REST_SERVER_INFO,
        REST_SERVER_CONFIG,
        REST_CLUSTER_CONFIG,
        ACCESS_KEY_LISTED,
        ACCESS_KEY_PUBLISHED,
        ACCESS_KEY_SUBMITTED,
        DEVICE_PUBLISHED,
        DEVICE_SUBMITTED,
        DEVICES_LISTED,
        DEVICE_TYPE_PUBLISHED,
        DEVICE_TYPES_LISTED,
        DEVICE_TYPE_SUBMITTED,
        COMMAND_TO_CLIENT,
        COMMAND_TO_DEVICE,
        COMMAND_LISTED,
        POST_COMMAND_TO_DEVICE,
        PLUGIN_PUBLISHED,
        PLUGIN_SUBMITTED,
        COMMAND_FROM_CLIENT,
        COMMAND_UPDATE_FROM_DEVICE,
        COMMAND_UPDATE_TO_CLIENT,
        NOTIFICATION_FROM_DEVICE,
        NOTIFICATION_TO_DEVICE,
        NOTIFICATION_TO_CLIENT,
        USER_PUBLISHED,
        USER_SUBMITTED,
        USERS_LISTED,
        NETWORK_PUBLISHED,
        NETWORKS_LISTED,
        NETWORK_SUBMITTED,
        SUBSCRIPTIONS_LISTED,
        PLUGINS_LISTED
    }
}
