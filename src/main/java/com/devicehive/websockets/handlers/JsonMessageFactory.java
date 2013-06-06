package com.devicehive.websockets.handlers;


import javax.json.Json;
import javax.json.JsonObjectBuilder;

public class JsonMessageFactory {

    public static JsonObjectBuilder createResponseBuilder(String requestId, String status) {
        return Json.createObjectBuilder()
            .add("requestId", requestId)
            .add("status", status);
    }

    public static JsonObjectBuilder createResponseBuilder(String requestId) {
        return Json.createObjectBuilder()
            .add("requestId", requestId);
    }

}
