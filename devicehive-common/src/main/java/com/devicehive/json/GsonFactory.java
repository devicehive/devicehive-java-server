package com.devicehive.json;

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


import com.devicehive.json.adapters.*;
import com.devicehive.json.strategies.AnnotatedStrategy;
import com.devicehive.model.enums.*;
import com.devicehive.model.eventbus.events.CommandEvent;
import com.devicehive.model.eventbus.events.CommandUpdateEvent;
import com.devicehive.model.eventbus.events.CommandsUpdateEvent;
import com.devicehive.model.eventbus.events.NotificationEvent;
import com.devicehive.model.rpc.*;
import com.devicehive.shim.api.Action;
import com.devicehive.shim.api.Body;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy;

public class GsonFactory {

    private static Map<Policy, Gson> cache = new ConcurrentHashMap<>();
    private static Gson gson = createGsonBuilder().create();

    public static Gson createGson() {
        return gson;
    }

    public static Gson createGson(Policy policy) {
        Gson gson = cache.get(policy);
        if (gson != null) {
            return gson;
        }
        gson = createGsonBuilder()
                .addDeserializationExclusionStrategy(new AnnotatedStrategy(policy))
                .addSerializationExclusionStrategy(new AnnotatedStrategy(policy))
                .create();
        cache.put(policy, gson);
        return gson;
    }


    private static GsonBuilder createGsonBuilder() {
        RuntimeTypeAdapterFactory<Body> req = RuntimeTypeAdapterFactory.of(Body.class, "a")
                .registerSubtype(ErrorResponse.class, Action.ERROR_RESPONSE.ordinal())

                .registerSubtype(CommandInsertRequest.class, Action.COMMAND_INSERT_REQUEST.ordinal())
                .registerSubtype(CommandInsertResponse.class, Action.COMMAND_INSERT_RESPONSE.ordinal())
                .registerSubtype(CommandSearchRequest.class, Action.COMMAND_SEARCH_REQUEST.ordinal())
                .registerSubtype(CommandSearchResponse.class, Action.COMMAND_SEARCH_RESPONSE.ordinal())
                .registerSubtype(CommandSubscribeRequest.class, Action.COMMAND_SUBSCRIBE_REQUEST.ordinal())
                .registerSubtype(CommandSubscribeResponse.class, Action.COMMAND_SUBSCRIBE_RESPONSE.ordinal())
                .registerSubtype(CommandUnsubscribeRequest.class, Action.COMMAND_UNSUBSCRIBE_REQUEST.ordinal())
                .registerSubtype(CommandUnsubscribeResponse.class, Action.COMMAND_UNSUBSCRIBE_RESPONSE.ordinal())
                .registerSubtype(CommandUpdateSubscribeRequest.class, Action.COMMAND_UPDATE_SUBSCRIBE_REQUEST.ordinal())
                .registerSubtype(CommandUpdateSubscribeResponse.class, Action.COMMAND_UPDATE_SUBSCRIBE_RESPONSE.ordinal())
                .registerSubtype(CommandUpdateRequest.class, Action.COMMAND_UPDATE_REQUEST.ordinal())
                .registerSubtype(CommandsUpdateRequest.class, Action.COMMANDS_UPDATE_REQUEST.ordinal())

                .registerSubtype(NotificationSearchRequest.class, Action.NOTIFICATION_SEARCH_REQUEST.ordinal())
                .registerSubtype(NotificationSearchResponse.class, Action.NOTIFICATION_SEARCH_RESPONSE.ordinal())
                .registerSubtype(NotificationInsertRequest.class, Action.NOTIFICATION_INSERT_REQUEST.ordinal())
                .registerSubtype(NotificationInsertResponse.class, Action.NOTIFICATION_INSERT_RESPONSE.ordinal())
                .registerSubtype(NotificationSubscribeRequest.class, Action.NOTIFICATION_SUBSCRIBE_REQUEST.ordinal())
                .registerSubtype(NotificationSubscribeResponse.class, Action.NOTIFICATION_SUBSCRIBE_RESPONSE.ordinal())
                .registerSubtype(NotificationUnsubscribeRequest.class, Action.NOTIFICATION_UNSUBSCRIBE_REQUEST.ordinal())
                .registerSubtype(NotificationUnsubscribeResponse.class, Action.NOTIFICATION_UNSUBSCRIBE_RESPONSE.ordinal())

                .registerSubtype(NotificationEvent.class, Action.NOTIFICATION_EVENT.ordinal())
                .registerSubtype(CommandEvent.class, Action.COMMAND_EVENT.ordinal())
                .registerSubtype(CommandUpdateEvent.class, Action.COMMAND_UPDATE_EVENT.ordinal())
                .registerSubtype(CommandsUpdateEvent.class, Action.COMMANDS_UPDATE_EVENT.ordinal())

                .registerSubtype(PluginSubscribeRequest.class, Action.PLUGIN_SUBSCRIBE_REQUEST.ordinal())
                .registerSubtype(PluginSubscribeResponse.class, Action.PLUGIN_SUBSCRIBE_RESPONSE.ordinal())
                .registerSubtype(PluginUnsubscribeRequest.class, Action.PLUGIN_UNSUBSCRIBE_REQUEST.ordinal())
                .registerSubtype(PluginUnsubscribeResponse.class, Action.PLUGIN_UNSUBSCRIBE_RESPONSE.ordinal())

                .registerSubtype(ListUserRequest.class, Action.LIST_USER_REQUEST.ordinal())
                .registerSubtype(ListUserResponse.class, Action.LIST_USER_RESPONSE.ordinal())
                .registerSubtype(CountUserRequest.class, Action.COUNT_USER_REQUEST.ordinal())

                .registerSubtype(ListNetworkRequest.class, Action.LIST_NETWORK_REQUEST.ordinal())
                .registerSubtype(ListNetworkResponse.class, Action.LIST_NETWORK_RESPONSE.ordinal())
                .registerSubtype(CountNetworkRequest.class, Action.COUNT_NETWORK_REQUEST.ordinal())

                .registerSubtype(ListDeviceTypeRequest.class, Action.LIST_DEVICE_TYPE_REQUEST.ordinal())
                .registerSubtype(ListDeviceTypeResponse.class, Action.LIST_DEVICE_TYPE_RESPONSE.ordinal())
                .registerSubtype(CountDeviceTypeRequest.class, Action.COUNT_DEVICE_TYPE_REQUEST.ordinal())

                .registerSubtype(ListDeviceRequest.class, Action.LIST_DEVICE_REQUEST.ordinal())
                .registerSubtype(ListDeviceResponse.class, Action.LIST_DEVICE_RESPONSE.ordinal())
                .registerSubtype(CountDeviceRequest.class, Action.COUNT_DEVICE_REQUEST.ordinal())

                .registerSubtype(ListPluginRequest.class, Action.LIST_PLUGIN_REQUEST.ordinal())
                .registerSubtype(ListPluginResponse.class, Action.LIST_PLUGIN_RESPONSE.ordinal())
                .registerSubtype(CountPluginRequest.class, Action.COUNT_PLUGIN_REQUEST.ordinal())

                .registerSubtype(DeviceDeleteRequest.class, Action.DEVICE_DELETE_REQUEST.ordinal())
                .registerSubtype(DeviceDeleteResponse.class, Action.DEVICE_DELETE_RESPONSE.ordinal())

                .registerSubtype(CountResponse.class, Action.COUNT_RESPONSE.ordinal());

        return new GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .registerTypeAdapterFactory(new OptionalAdapterFactory())
                .registerTypeAdapterFactory(new JsonStringWrapperAdapterFactory())
                .registerTypeAdapter(Date.class, new TimestampAdapter())
                .registerTypeAdapter(UserRole.class, new UserRoleAdapter())
                .registerTypeAdapter(UserStatus.class, new UserStatusAdapter())
                .registerTypeAdapterFactory(req);
    }

}
