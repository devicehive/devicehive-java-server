package com.devicehive.websockets.handlers;



import com.devicehive.model.*;
import com.devicehive.websockets.handlers.annotations.Action;
import com.devicehive.websockets.json.GsonFactory;
import com.devicehive.websockets.subscriptions.NotificationsSubscriptionManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.websocket.Session;
import java.util.Date;
import java.util.UUID;

@Singleton
public class DeviceMessageHandlers implements HiveMessageHandlers {

    @Inject
    private NotificationsSubscriptionManager notificationsSubscriptionManager;

    @Action(value = "authenticate", needsAuth = false)
    public JsonObject processAuthenticate(JsonObject message, Session session) {
        UUID deviceId = GsonFactory.createGson().fromJson(message.get("deviceId"), UUID.class);
        String deviceKey = message.get("deviceKey").getAsString();

        //TODO session auth
        JsonObject jsonObject = JsonMessageFactory.createSuccessResponse();
        return jsonObject;
    }

    @Action(value = "command/update")
    public JsonObject processCommandUpdate(JsonObject message, Session session) {
        Integer commandId = message.get("commandId").getAsInt();
        DeviceCommand deviceCommand = GsonFactory.createGson().fromJson(message.getAsJsonObject("command"), DeviceCommand.class);


        //TODO update
        JsonObject jsonObject = JsonMessageFactory.createSuccessResponse();
        return jsonObject;
    }

    @Action(value = "command/subscribe")
    public JsonObject processNotificationSubscribe(JsonObject message, Session session) {
        Date timestamp = GsonFactory.createGson().fromJson(message.getAsJsonPrimitive("timestamp"), Date.class);
        //TODO subscribe
        JsonObject jsonObject = JsonMessageFactory.createSuccessResponse();
        return jsonObject;
    }

    @Action(value = "command/unsubscribe")
    public JsonObject processNotificationUnsubscribe(JsonObject message, Session session) {
        //TODO unsubscribe
        JsonObject jsonObject = JsonMessageFactory.createSuccessResponse();
        return jsonObject;
    }

    @Action(value = "notification/insert")
    public JsonObject processNotificationInsert(JsonObject message, Session session) {
        //TODO insert
        String status = null;
        JsonObject jsonObject = JsonMessageFactory.createSuccessResponse();
        jsonObject.add("notification", new JsonObject());
        return jsonObject;
    }

    @Action(value = "server/info")
    public JsonObject processServerInfo(JsonObject message, Session session) {
        JsonObject jsonObject = JsonMessageFactory.createSuccessResponse();
        ApiInfo apiInfo = new ApiInfo();
        apiInfo.setApiVersion(Version.VERSION);
        apiInfo.setServerTimestamp(new Date());
        apiInfo.setWebSocketServerUrl("TODO_URL");
        jsonObject.add("info", GsonFactory.createGson().toJsonTree(apiInfo));
        return jsonObject;
    }

    @Action(value = "device/get")
    public JsonObject processDeviceGet(JsonObject message, Session session) {
        //TODO get
        JsonObject jsonObject = JsonMessageFactory.createSuccessResponse();
        jsonObject.add("device", new JsonObject());
        return jsonObject;
    }

    @Action(value = "device/save", needsAuth = false)
    public JsonObject processDeviceSave(JsonObject message, Session session) {
        UUID deviceId = GsonFactory.createGson().fromJson(message.get("deviceId"), UUID.class);
        String deviceKey = message.get("deviceKey").getAsString();

         Device device = GsonFactory.createGson().fromJson(message.get("device"), Device.class);

        //TODO
        JsonObject jsonObject = JsonMessageFactory.createSuccessResponse();
        return jsonObject;
    }

}
