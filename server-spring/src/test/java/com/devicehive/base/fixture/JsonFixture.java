package com.devicehive.base.fixture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.tuple.Pair;

public class JsonFixture {

    public static JsonObject createWsCommand(String action, String request, String deviceId, String deviceKey, Pair<String, JsonElement> data) {
        JsonObject command = new JsonObject();
        command.add("action", new JsonPrimitive(action));
        command.add("request", new JsonPrimitive(request));
        command.add("deviceId", new JsonPrimitive(deviceId));
        command.add("deviceKey", new JsonPrimitive(deviceKey));
        command.add(data.getKey(), data.getValue());
        return command;
    }

}
