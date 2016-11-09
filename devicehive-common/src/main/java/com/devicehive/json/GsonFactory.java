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
import com.devicehive.model.eventbus.events.NotificationEvent;
import com.devicehive.model.rpc.*;
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
        RuntimeTypeAdapterFactory<Body> req = RuntimeTypeAdapterFactory.of(Body.class, "action")
                .registerSubtype(ErrorResponse.class, Action.ERROR_RESPONSE.name())

                .registerSubtype(CommandInsertRequest.class, Action.COMMAND_INSERT_REQUEST.name())
                .registerSubtype(CommandInsertResponse.class, Action.COMMAND_INSERT_RESPONSE.name())
                .registerSubtype(CommandSearchRequest.class, Action.COMMAND_SEARCH_REQUEST.name())
                .registerSubtype(CommandSearchResponse.class, Action.COMMAND_SEARCH_RESPONSE.name())
                .registerSubtype(CommandSubscribeRequest.class, Action.COMMAND_SUBSCRIBE_REQUEST.name())
                .registerSubtype(CommandSubscribeResponse.class, Action.COMMAND_SUBSCRIBE_RESPONSE.name())
                .registerSubtype(CommandUnsubscribeRequest.class, Action.COMMAND_UNSUBSCRIBE_REQUEST.name())
                .registerSubtype(CommandUnsubscribeResponse.class, Action.COMMAND_UNSUBSCRIBE_RESPONSE.name())
                .registerSubtype(CommandUpdateSubscribeRequest.class, Action.COMMAND_UPDATE_SUBSCRIBE_REQUEST.name())
                .registerSubtype(CommandUpdateSubscribeResponse.class, Action.COMMAND_UPDATE_SUBSCRIBE_RESPONSE.name())
                .registerSubtype(CommandUpdateRequest.class, Action.COMMAND_UPDATE_REQUEST.name())
                .registerSubtype(CommandGetSubscriptionRequest.class, Action.COMMAND_GET_SUBSCRIPTION_REQUEST.name())
                .registerSubtype(CommandGetSubscriptionResponse.class, Action.COMMAND_GET_SUBSCRIPTION_RESPONSE.name())

                .registerSubtype(NotificationSearchRequest.class, Action.NOTIFICATION_SEARCH_REQUEST.name())
                .registerSubtype(NotificationSearchResponse.class, Action.NOTIFICATION_SEARCH_RESPONSE.name())
                .registerSubtype(NotificationInsertRequest.class, Action.NOTIFICATION_INSERT_REQUEST.name())
                .registerSubtype(NotificationInsertResponse.class, Action.NOTIFICATION_INSERT_RESPONSE.name())
                .registerSubtype(NotificationSubscribeRequest.class, Action.NOTIFICATION_SUBSCRIBE_REQUEST.name())
                .registerSubtype(NotificationSubscribeResponse.class, Action.NOTIFICATION_SUBSCRIBE_RESPONSE.name())
                .registerSubtype(NotificationUnsubscribeRequest.class, Action.NOTIFICATION_UNSUBSCRIBE_REQUEST.name())
                .registerSubtype(NotificationUnsubscribeResponse.class, Action.NOTIFICATION_UNSUBSCRIBE_RESPONSE.name())

                .registerSubtype(NotificationEvent.class, Action.NOTIFICATION_EVENT.name())
                .registerSubtype(CommandEvent.class, Action.COMMAND_EVENT.name())
                .registerSubtype(CommandUpdateEvent.class, Action.COMMAND_UPDATE_EVENT.name())

                .registerSubtype(ListAccessKeyRequest.class, Action.LIST_ACCESS_KEY_REQUEST.name())
                .registerSubtype(ListAccessKeyResponse.class, Action.LIST_ACCESS_KEY_RESPONSE.name())

                .registerSubtype(ListUserRequest.class, Action.LIST_USER_REQUEST.name())
                .registerSubtype(ListUserResponse.class, Action.LIST_USER_RESPONSE.name())

                .registerSubtype(ListDeviceClassRequest.class, Action.LIST_DEVICE_CLASS_REQUEST.name())
                .registerSubtype(ListDeviceClassResponse.class, Action.LIST_DEVICE_CLASS_RESPONSE.name())

                .registerSubtype(ListNetworkRequest.class, Action.LIST_NETWORK_REQUEST.name())
                .registerSubtype(ListNetworkResponse.class, Action.LIST_NETWORK_RESPONSE.name())

                .registerSubtype(ListDeviceRequest.class, Action.LIST_DEVICE_REQUEST.name())
                .registerSubtype(ListDeviceResponse.class, Action.LIST_DEVICE_RESPONSE.name());

        return new GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .registerTypeAdapterFactory(new OptionalAdapterFactory())
                .registerTypeAdapterFactory(new JsonStringWrapperAdapterFactory())
                .registerTypeAdapter(Date.class, new TimestampAdapter())
                .registerTypeAdapter(UserRole.class, new UserRoleAdapter())
                .registerTypeAdapter(UserStatus.class, new UserStatusAdapter())
                .registerTypeAdapter(Type.class, new OAuthTypeAdapter())
                .registerTypeAdapter(AccessType.class, new AccessTypeAdapter())
                .registerTypeAdapter(AccessKeyType.class, new AccessKeyStatusAdapter())
                .registerTypeAdapterFactory(req);
    }

}
