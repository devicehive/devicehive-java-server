package com.devicehive.websockets.json;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;
import java.util.UUID;

public class ConvertorFactory {

    public static Gson createGson() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Date.class, new DateAdapter());
        builder.registerTypeAdapter(UUID.class, new UUIDAdapter());
        builder.setPrettyPrinting();
        return  builder.create();
    }
}
