package com.devicehive.websockets.handlers;



import com.devicehive.model.AuthLevel;

import javax.json.JsonObject;
import javax.websocket.Session;

public class DeviceMessageHandlers implements HiveMessageHandlers {

    @Action(value = "authenticate")
    public JsonObject processAuthenticate(JsonObject message, Session session) {
        return null;
    }

    @Action(value = "command/update", requredLevel = AuthLevel.DEVICE)
    public JsonObject processCommandUpdate(JsonObject message, Session session) {
        return null;
    }

    @Action(value = "notification/subscribe", requredLevel = AuthLevel.DEVICE)
    public JsonObject processNotificationSubscribe(JsonObject message, Session session) {
        return null;
    }

    @Action(value = "notification/unsubscribe", requredLevel = AuthLevel.DEVICE)
    public JsonObject processNotificationUnsubscribe(JsonObject message, Session session) {
        return null;
    }

    @Action(value = "notification/insert", requredLevel = AuthLevel.DEVICE)
    public JsonObject processNotificationInsert(JsonObject message, Session session) {
        return null;
    }

    @Action(value = "server/info")
    public JsonObject processServerInfo(JsonObject message, Session session) {
        return null;
    }

    @Action(value = "device/get", requredLevel = AuthLevel.DEVICE)
    public JsonObject processDeviceGet(JsonObject message, Session session) {
        return null;
    }

    @Action(value = "device/save", requredLevel = AuthLevel.NONE)
    public JsonObject processDeviceSave(JsonObject message, Session session) {
        return null;
    }

    /*

    @Action(value = "command/insert")
    public JsonObject processCommandInsert(JsonObject message, Session session) {
        return null;
    }
     */
}
