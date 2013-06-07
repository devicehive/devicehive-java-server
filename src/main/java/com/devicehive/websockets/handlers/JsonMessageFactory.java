package com.devicehive.websockets.handlers;


import com.google.gson.JsonObject;

public class JsonMessageFactory {

    public static JsonObject createStatusResponce(String status) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("status", status);
        return jsonObject;
    }


}
