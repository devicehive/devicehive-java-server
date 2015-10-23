package com.devicehive.base.fixture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Map;

public class JsonFixture {

    public static JsonObject createWsCommand(String action, String request, String deviceId, Map<String, JsonElement> otherFields) {
        JsonObject command = createWsCommand(action, request, deviceId);
        otherFields.entrySet().stream().forEach(entry -> command.add(entry.getKey(), entry.getValue()));
        return command;
    }

    public static JsonObject createWsCommand(String action, String request, String deviceId) {
        JsonObject command = new JsonObject();
        command.add("action", new JsonPrimitive(action));
        command.add("requestId", new JsonPrimitive(request));
        command.add("deviceGuid", new JsonPrimitive(deviceId));
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

    public static JsonObject userAuthCommand(String request, String login, String password) {
        JsonObject command = new JsonObject();
        command.add("action", new JsonPrimitive("authenticate"));
        command.add("requestId", new JsonPrimitive(request));
        command.add("login", new JsonPrimitive(login));
        command.add("password", new JsonPrimitive(password));
        return command;
    }

    public static JsonObject keyAuthCommand(String request, String accessKey) {
        JsonObject command = new JsonObject();
        command.add("action", new JsonPrimitive("authenticate"));
        command.add("requestId", new JsonPrimitive(request));
        command.add("accessKey", new JsonPrimitive(accessKey));
        return command;
    }
}
