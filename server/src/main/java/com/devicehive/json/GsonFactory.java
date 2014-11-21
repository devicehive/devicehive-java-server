package com.devicehive.json;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.devicehive.json.adapters.AccessTypeAdapter;
import com.devicehive.json.adapters.JsonStringWrapperAdapterFactory;
import com.devicehive.json.adapters.NullableWrapperAdapterFactory;
import com.devicehive.json.adapters.OAuthTypeAdapter;
import com.devicehive.json.adapters.TimestampAdapter;
import com.devicehive.json.adapters.UserRoleAdapter;
import com.devicehive.json.adapters.UserStatusAdapter;
import com.devicehive.json.strategies.AnnotatedStrategy;
import com.devicehive.model.enums.AccessType;
import com.devicehive.model.enums.Type;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.enums.UserStatus;

import java.sql.Timestamp;
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
        return new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .registerTypeAdapterFactory(new NullableWrapperAdapterFactory())
            .registerTypeAdapterFactory(new JsonStringWrapperAdapterFactory())
            .registerTypeAdapter(Timestamp.class, new TimestampAdapter())
            .registerTypeAdapter(UserRole.class, new UserRoleAdapter())
            .registerTypeAdapter(UserStatus.class, new UserStatusAdapter())
            .registerTypeAdapter(Type.class, new OAuthTypeAdapter())
            .registerTypeAdapter(AccessType.class, new AccessTypeAdapter());
    }

}
