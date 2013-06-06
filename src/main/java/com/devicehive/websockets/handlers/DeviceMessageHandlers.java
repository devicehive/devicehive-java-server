package com.devicehive.websockets.handlers;



import com.devicehive.model.AuthLevel;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.websocket.Session;

public class DeviceMessageHandlers implements HiveMessageHandlers {

    @Action(value = "authenticate", copyRequestId = true)
    public JsonObject processAuthenticate(JsonObject message, Session session) {
        //TODO
        String status = null;
        return JsonMessageFactory.createResponseBuilder( status).build();
    }

    @Action(value = "command/update", requredLevel = AuthLevel.DEVICE, copyRequestId = true)
    public JsonObject processCommandUpdate(JsonObject message, Session session) {
        //TODO
        String status = null;
        return JsonMessageFactory.createResponseBuilder(status).build();
    }

    @Action(value = "notification/subscribe", requredLevel = AuthLevel.DEVICE, copyRequestId = true)
    public JsonObject processNotificationSubscribe(JsonObject message, Session session) {
        //TODO
        String status = null;
        return JsonMessageFactory.createResponseBuilder( status).build();
    }

    @Action(value = "notification/unsubscribe", requredLevel = AuthLevel.DEVICE, copyRequestId = true)
    public JsonObject processNotificationUnsubscribe(JsonObject message, Session session) {
        //TODO
        String status = null;
        return JsonMessageFactory.createResponseBuilder( status).build();
    }

    @Action(value = "notification/insert", requredLevel = AuthLevel.DEVICE, copyRequestId = true)
    public JsonObject processNotificationInsert(JsonObject message, Session session) {
        //TODO
        JsonObjectBuilder builder = JsonMessageFactory.createResponseBuilder("success");
        builder.add("notification", Json.createObjectBuilder());
        return  builder.build();
    }

    @Action(value = "server/info", copyRequestId = true)
    public JsonObject processServerInfo(JsonObject message, Session session) {
        //TODO
        JsonObjectBuilder builder = JsonMessageFactory.createResponseBuilder("success");
        builder.add("info", Json.createObjectBuilder());
        return  builder.build();
    }

    @Action(value = "device/get", requredLevel = AuthLevel.DEVICE, copyRequestId = true)
    public JsonObject processDeviceGet(JsonObject message, Session session) {
        //TODO
        JsonObjectBuilder builder = JsonMessageFactory.createResponseBuilder( "success");
        builder.add("device", Json.createObjectBuilder());
        return  builder.build();
    }

    @Action(value = "device/save", requredLevel = AuthLevel.NONE)
    public JsonObject processDeviceSave(JsonObject message, Session session) {
        //TODO
        String status = null;
        return JsonMessageFactory.createResponseBuilder(status).build();
    }

    /*

    @Action(value = "command/insert")
    public JsonObject processCommandInsert(JsonObject message, Session session) {
        return null;
    }
     */
}
