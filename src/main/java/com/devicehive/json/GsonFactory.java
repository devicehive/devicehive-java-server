package com.devicehive.json;


import com.devicehive.model.JsonStringWrapper;
import com.devicehive.json.adapters.DateAdapter;
import com.devicehive.json.adapters.JsonDbObjectAdapter;
import com.devicehive.json.adapters.UUIDAdapter;
import com.devicehive.json.strategies.AnnotatedStrategy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
                .registerTypeAdapter(Date.class, new DateAdapter())
                .registerTypeAdapter(UUID.class, new UUIDAdapter())
                .registerTypeAdapter(JsonStringWrapper.class, new JsonDbObjectAdapter());
    }

}
