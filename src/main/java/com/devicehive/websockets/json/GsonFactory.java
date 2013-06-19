package com.devicehive.websockets.json;


import com.google.gson.ExclusionStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;
import java.util.UUID;

public class GsonFactory {

    public static Gson createGson() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Date.class, new DateAdapter());
        builder.registerTypeAdapter(UUID.class, new UUIDAdapter());
        builder.setPrettyPrinting();
        return  builder.create();
    }

    public static Gson createGson(ExclusionStrategy serializationExclusionStrategy) {
        GsonBuilder builder = new GsonBuilder();
        builder.addDeserializationExclusionStrategy(serializationExclusionStrategy);
        builder.addSerializationExclusionStrategy(serializationExclusionStrategy);
        builder.registerTypeAdapter(Date.class, new DateAdapter());
        builder.registerTypeAdapter(UUID.class, new UUIDAdapter());
        builder.setPrettyPrinting();
        return  builder.create();
    }

    public static Gson createGson(ExclusionStrategy serializationExclusionStrategy, ExclusionStrategy deserializationExclusionStrategy) {
        GsonBuilder builder = new GsonBuilder();
        builder.addDeserializationExclusionStrategy(deserializationExclusionStrategy);
        builder.addSerializationExclusionStrategy(serializationExclusionStrategy);
        builder.registerTypeAdapter(Date.class, new DateAdapter());
        builder.registerTypeAdapter(UUID.class, new UUIDAdapter());
        builder.setPrettyPrinting();
        return  builder.create();
    }
}
