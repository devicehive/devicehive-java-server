package com.devicehive.json;


import com.devicehive.json.adapters.*;
import com.devicehive.json.strategies.AnnotatedStrategy;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.UserRole;
import com.devicehive.model.UserStatus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy;
public class GsonFactory {

    public static Gson createGson() {
        return createGsonBuilder()
                .create();
    }

    public static Gson createGson(Policy policy) {
        return createGsonBuilder()
                .addDeserializationExclusionStrategy(new AnnotatedStrategy(policy))
                .addSerializationExclusionStrategy(new AnnotatedStrategy(policy))
                .create();
    }


    public static GsonBuilder createGsonBuilder() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .registerTypeAdapterFactory(new NullableWrapperAdapterFactory())
                .registerTypeAdapter(Date.class, new DateAdapter())
                .registerTypeAdapter(UUID.class, new UUIDAdapter())
                .registerTypeAdapter(Timestamp.class, new TimestampAdapter())
                .registerTypeAdapter(JsonStringWrapper.class, new JsonDbObjectAdapter())
                .registerTypeAdapter(UserRole.class, new UserRoleAdapter())
                .registerTypeAdapter(UserStatus.class, new UserStatusAdapter());
    }

}
