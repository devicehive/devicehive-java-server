package com.devicehive.websockets.handlers;




import com.devicehive.model.AuthLevel;

import javax.json.JsonObject;
import javax.websocket.Session;

public class ClientMessageHandlers implements HiveMessageHandlers {

    @Action(value = "authenticate")
    public JsonObject processAuthenticate(JsonObject message, Session session) {
        return null;
    }


    @Action(value = "command/update")
    public JsonObject processCommandUpdate(JsonObject message, Session session) {
        return null;
    }

    @Action(value = "notification/subscribe", requredLevel = AuthLevel.USER)
    public JsonObject processNotificationSubscribe(JsonObject message, Session session) {
        return null;
    }

    @Action(value = "notification/unsubscribe", requredLevel = AuthLevel.USER)
    public JsonObject processNotificationUnsubscribe(JsonObject message, Session session) {
        return null;
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


    @Action(value = "server/info")
    public JsonObject processServerInfo(JsonObject message, Session session) {
        return null;
    }
}
