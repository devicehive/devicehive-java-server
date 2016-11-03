package com.devicehive.base.fixture;

/*
 * #%L
 * DeviceHive Java Server Common business logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
        command.add("deviceId", new JsonPrimitive(deviceId));
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
