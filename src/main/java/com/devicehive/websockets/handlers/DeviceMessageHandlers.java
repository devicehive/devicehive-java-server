package com.devicehive.websockets.handlers;



import com.devicehive.model.AuthLevel;
import com.google.gson.JsonObject;

import javax.websocket.Session;

public class DeviceMessageHandlers implements HiveMessageHandlers {

    @Action(value = "authenticate", copyRequestId = true)
    public JsonObject processAuthenticate(JsonObject message, Session session) {
        //TODO
        String status = null;
        JsonObject jsonObject = JsonMessageFactory.createStatusResponce(status);
        return jsonObject;
    }

    @Action(value = "command/update", requredLevel = AuthLevel.DEVICE, copyRequestId = true)
    public JsonObject processCommandUpdate(JsonObject message, Session session) {
        //TODO
        String status = null;
        JsonObject jsonObject = JsonMessageFactory.createStatusResponce(status);
        return jsonObject;
    }

    @Action(value = "notification/subscribe", requredLevel = AuthLevel.DEVICE, copyRequestId = true)
    public JsonObject processNotificationSubscribe(JsonObject message, Session session) {
        //TODO
        String status = null;
        JsonObject jsonObject = JsonMessageFactory.createStatusResponce(status);
        return jsonObject;
    }

    @Action(value = "notification/unsubscribe", requredLevel = AuthLevel.DEVICE, copyRequestId = true)
    public JsonObject processNotificationUnsubscribe(JsonObject message, Session session) {
        //TODO
        String status = null;
        JsonObject jsonObject = JsonMessageFactory.createStatusResponce(status);
        return jsonObject;
    }

    @Action(value = "notification/insert", requredLevel = AuthLevel.DEVICE, copyRequestId = true)
    public JsonObject processNotificationInsert(JsonObject message, Session session) {
        //TODO
        String status = null;
        JsonObject jsonObject = JsonMessageFactory.createStatusResponce(status);
        jsonObject.add("notification", new JsonObject());
        return jsonObject;
    }

    @Action(value = "server/info", copyRequestId = true)
    public JsonObject processServerInfo(JsonObject message, Session session) {
        //TODO
        String status = null;
        JsonObject jsonObject = JsonMessageFactory.createStatusResponce(status);
        jsonObject.add("info", new JsonObject());
        return jsonObject;
    }

    @Action(value = "device/get", requredLevel = AuthLevel.DEVICE, copyRequestId = true)
    public JsonObject processDeviceGet(JsonObject message, Session session) {
        //TODO
        String status = null;
        JsonObject jsonObject = JsonMessageFactory.createStatusResponce(status);
        jsonObject.add("device", new JsonObject());
        return jsonObject;
    }

    @Action(value = "device/save", requredLevel = AuthLevel.NONE)
    public JsonObject processDeviceSave(JsonObject message, Session session) {
        //TODO
        String status = null;
        JsonObject jsonObject = JsonMessageFactory.createStatusResponce(status);
        return jsonObject;
    }

    /*

    @Action(value = "command/insert")
    public JsonObject processCommandInsert(JsonObject message, Session session) {
        return null;
    }
     */
}
