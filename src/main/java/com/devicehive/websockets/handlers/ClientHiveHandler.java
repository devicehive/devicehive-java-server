package com.devicehive.websockets.handlers;




import javax.json.JsonObject;
import javax.websocket.Session;

public class ClientHiveHandler implements HiveMessageHandler {

    @Action(value = "authenticate")
    public JsonObject processAuthenticate(JsonObject message, Session session) {
        return null;
    }

    @Action(value = "command/insert")
    public JsonObject processCommandInsert(JsonObject message, Session session) {
        return null;
    }

    @Action(value = "command/update")
    public JsonObject processCommandUpdate(JsonObject message, Session session) {
        return null;
    }

    @Action(value = "notification/subscribe")
    public JsonObject processNotificationSubscribe(JsonObject message, Session session) {
        return null;
    }

    @Action(value = "notification/unsubscribe")
    public JsonObject processNotificationUnsubscribe(JsonObject message, Session session) {
        return null;
    }


    @Action(value = "notification/insert")
    public JsonObject processNotificationInsert(JsonObject message, Session session) {
        return null;
    }

    @Action(value = "server/info")
    public JsonObject processServerInfo(JsonObject message, Session session) {
        return null;
    }
}
