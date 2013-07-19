package com.devicehive.websockets.json;


import com.devicehive.model.JsonStringWrapper;
import com.devicehive.websockets.json.strategies.AnnotatedStrategy;
import com.google.gson.ExclusionStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import java.lang.annotation.Annotation;
import java.util.Date;
import java.util.UUID;

public class GsonFactory {

    public static Gson createGson() {
        return createGsonBuilder()
                .create();
    }

    public static Gson createGson(Class<? extends Annotation> annotationClass) {
        return createGsonBuilder()
                .addDeserializationExclusionStrategy(new AnnotatedStrategy(annotationClass))
                .addSerializationExclusionStrategy(new AnnotatedStrategy(annotationClass))
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
