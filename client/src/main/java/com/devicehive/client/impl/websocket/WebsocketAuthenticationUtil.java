package com.devicehive.client.impl.websocket;


import com.devicehive.client.impl.context.HiveContext;
import com.google.gson.JsonObject;

import java.util.UUID;

public class WebsocketAuthenticationUtil {

    public static void authenticateDevice(String deviceId, String deviceKey, HiveContext hiveContext){
        JsonObject request = new JsonObject();
        request.addProperty("action", "authenticate");
        String requestId = UUID.randomUUID().toString();
        request.addProperty("requestId", requestId);
        request.addProperty("deviceId", deviceId);
        request.addProperty("deviceKey", deviceKey);
        hiveContext.getHiveWebSocketClient().sendMessage(request);
    }

    public static void authenticateClient(String login, String password, HiveContext hiveContext){
        JsonObject request = new JsonObject();
        request.addProperty("action", "authenticate");
        String requestId = UUID.randomUUID().toString();
        request.addProperty("requestId", requestId);
        request.addProperty("login", login);
        request.addProperty("password", password);
        hiveContext.getHiveWebSocketClient().sendMessage(request);
    }

    public static void authenticateKey(String key, HiveContext hiveContext){
        JsonObject request = new JsonObject();
        request.addProperty("action", "authenticate");
        String requestId = UUID.randomUUID().toString();
        request.addProperty("requestId", requestId);
        request.addProperty("accessKey", key);
        hiveContext.getHiveWebSocketClient().sendMessage(request);
    }
}
