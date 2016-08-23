package com.devicehive.json;


import com.devicehive.json.adapters.*;
import com.devicehive.json.strategies.AnnotatedStrategy;
import com.devicehive.model.enums.*;
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
                .registerSubtype(EchoRequest.class, Action.ECHO_REQUEST.name())
                .registerSubtype(EchoResponse.class, Action.ECHO_RESPONSE.name())
                .registerSubtype(CommandSearchRequest.class, Action.COMMAND_SEARCH_REQUEST.name())
                .registerSubtype(CommandSearchResponse.class, Action.COMMAND_SEARCH_RESPONSE.name())
                .registerSubtype(NotificationSearchRequest.class, Action.NOTIFICATION_SEARCH_REQUEST.name())
                .registerSubtype(NotificationSearchResponse.class, Action.NOTIFICATION_SEARCH_RESPONSE.name())
                .registerSubtype(NotificationInsertRequest.class, Action.NOTIFICATION_INSERT.name())
                .registerSubtype(SubscribeRequest.class, Action.NOTIFICATION_SUBSCRIBE_REQUEST.name())
                .registerSubtype(SubscribeResponse.class, Action.NOTIFICATION_SUBSCRIBE_RESPONSE.name());

        return new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
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
