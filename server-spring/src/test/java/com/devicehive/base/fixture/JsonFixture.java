package com.devicehive.base.fixture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

public class JsonFixture {

    public static JsonObject createWsCommand(String action, String request, String deviceId, String deviceKey, Pair<String, JsonElement> data) {
        JsonObject command = createWsCommand(action, request, deviceId, deviceKey);
        command.add(data.getKey(), data.getValue());
        return command;
    }

    public static JsonObject createWsCommand(String action, String request, String deviceId, String deviceKey) {
        JsonObject command = new JsonObject();
        command.add("action", new JsonPrimitive(action));
        command.add("requestId", new JsonPrimitive(request));
        command.add("deviceId", new JsonPrimitive(deviceId));
        command.add("deviceKey", new JsonPrimitive(deviceKey));
        return command;
    }

    public static JsonObject createWsCommand(String action, String request) {
        JsonObject command = new JsonObject();
        command.add("action", new JsonPrimitive(action));
        command.add("requestId", new JsonPrimitive(request));
        return command;
    }

    public static JsonObject createWsCommand(String action, String request, Map<String, JsonElement> otherFields) {
        JsonObject command = new JsonObject();
        command.add("action", new JsonPrimitive(action));
        command.add("requestId", new JsonPrimitive(request));
        otherFields.entrySet().stream().forEach(entry -> command.add(entry.getKey(), entry.getValue()));
        return command;
    }
}
