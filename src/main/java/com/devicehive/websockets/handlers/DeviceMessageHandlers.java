package com.devicehive.websockets.handlers;



import com.devicehive.model.ApiInfo;
import com.devicehive.model.AuthLevel;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.Version;
import com.devicehive.websockets.handlers.annotations.Action;
import com.devicehive.websockets.json.GsonFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.websocket.Session;
import java.util.Date;
import java.util.UUID;

public class DeviceMessageHandlers implements HiveMessageHandlers {

    private Gson gson = GsonFactory.createGson();

    @Action(value = "authenticate", needsAuth = false)
    public JsonObject processAuthenticate(JsonObject message, Session session) {
        UUID deviceId = gson.fromJson(message.get("deviceId"), UUID.class);
        String deviceKey = message.get("deviceKey").getAsString();

        //TODO session auth
        String status = null;
        JsonObject jsonObject = JsonMessageFactory.createStatusResponce(status);
        return jsonObject;
    }

    @Action(value = "command/update")
    public JsonObject processCommandUpdate(UUID deviceId, String deviceKey,JsonObject message, Session session) {
        Integer commandId = message.get("commandId").getAsInt();
        DeviceCommand deviceCommand = gson.fromJson(message.getAsJsonObject("command"), DeviceCommand.class);


        //TODO update
        String status = null;
        JsonObject jsonObject = JsonMessageFactory.createStatusResponce(status);
        return jsonObject;
    }

    @Action(value = "command/subscribe")
    public JsonObject processNotificationSubscribe(JsonObject message, Session session) {
        Date timestamp = gson.fromJson(message.getAsJsonPrimitive("timestamp"), Date.class);
        //TODO subscribe
        String status = null;
        JsonObject jsonObject = JsonMessageFactory.createStatusResponce(status);
        return jsonObject;
    }

    @Action(value = "command/unsubscribe")
    public JsonObject processNotificationUnsubscribe(JsonObject message, Session session) {
        //TODO unsubscribe
        String status = null;
        JsonObject jsonObject = JsonMessageFactory.createStatusResponce(status);
        return jsonObject;
    }

    @Action(value = "notification/insert")
    public JsonObject processNotificationInsert(JsonObject message, Session session) {
        //TODO insert
        String status = null;
        JsonObject jsonObject = JsonMessageFactory.createStatusResponce(status);
        jsonObject.add("notification", new JsonObject());
        return jsonObject;
    }

    @Action(value = "server/info")
    public JsonObject processServerInfo(JsonObject message, Session session) {
        JsonObject jsonObject = JsonMessageFactory.createStatusResponce("success");
        ApiInfo apiInfo = new ApiInfo();
        apiInfo.setApiVersion(Version.VERSION);
        apiInfo.setServerTimestamp(new Date());
        apiInfo.setWebSocketServerUrl("TODO_URL");
        jsonObject.add("info", gson.toJsonTree(apiInfo));
        return jsonObject;
    }

    @Action(value = "device/get")
    public JsonObject processDeviceGet(JsonObject message, Session session) {
        //TODO get
        String status = null;
        JsonObject jsonObject = JsonMessageFactory.createStatusResponce(status);
        jsonObject.add("device", new JsonObject());
        return jsonObject;
    }

    @Action(value = "device/save", needsAuth = false)
    public JsonObject processDeviceSave(JsonObject message, Session session) {
        UUID deviceId = gson.fromJson(message.get("deviceId"), UUID.class);
        String deviceKey = message.get("deviceKey").getAsString();

        //TODO
        String status = null;
        JsonObject jsonObject = JsonMessageFactory.createStatusResponce(status);
        return jsonObject;
    }

}
