package com.devicehive.websockets.handlers;




import com.devicehive.model.AuthLevel;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.websocket.Session;

public class ClientMessageHandlers implements HiveMessageHandlers {

    @Action(value = "authenticate", copyRequestId = true)
    public JsonObject processAuthenticate(JsonObject message, Session session) {
        String status = null;
        //TODO
        return JsonMessageFactory.createResponseBuilder(status).build();
    }


    @Action(value = "command/insert", copyRequestId = true)
    public JsonObject processCommandInsert(JsonObject message, Session session) {
        //TODO
        JsonObjectBuilder builder = JsonMessageFactory.createResponseBuilder("success");
        builder.add("command", Json.createObjectBuilder());
        return  builder.build();
    }

    @Action(value = "notification/subscribe", requredLevel = AuthLevel.USER, copyRequestId = true)
    public JsonObject processNotificationSubscribe(JsonObject message, Session session) {
        //TODO
        String status = null;
        JsonArray devices = message.getJsonArray("deviceGuids");
        return JsonMessageFactory.createResponseBuilder(status).build();
    }

    @Action(value = "notification/unsubscribe", requredLevel = AuthLevel.USER, copyRequestId = true)
    public JsonObject processNotificationUnsubscribe(JsonObject message, Session session) {
        //TODO
        String status = null;
        JsonArray devices = message.getJsonArray("deviceGuids");
        return JsonMessageFactory.createResponseBuilder(status).build();
    }

    /*
    @Action(value = "notification/insert")
    public JsonObject processNotificationInsert(JsonObject message, Session session) {
        return null;
    }
    @Action(value = "command/update")
    public JsonObject processCommandUpdate(JsonObject message, Session session) {
        return null;
    }*/


    @Action(value = "server/info", copyRequestId = true)
    public JsonObject processServerInfo(JsonObject message, Session session) {
        //TODO
        JsonObjectBuilder builder = JsonMessageFactory.createResponseBuilder("success");
        builder.add("info", Json.createObjectBuilder());
        return  builder.build();
    }
}
