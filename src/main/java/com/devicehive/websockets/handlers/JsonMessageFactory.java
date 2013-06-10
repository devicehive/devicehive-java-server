package com.devicehive.websockets.handlers;


import com.google.gson.JsonObject;

public class JsonMessageFactory {

    private static final String STATUS = "status";

    public static JsonObject createSuccessResponse() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(STATUS, "success");
        return jsonObject;
    }


    public static JsonObject createErrorResponse() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(STATUS, "error");
        return jsonObject;
    }


}
