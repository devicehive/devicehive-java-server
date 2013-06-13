package com.devicehive.websockets.handlers;



import com.devicehive.model.*;
import com.devicehive.websockets.handlers.annotations.Action;
import com.devicehive.websockets.json.GsonFactory;
import com.devicehive.websockets.messagebus.CommandsSubscriptionManager;
import com.devicehive.websockets.messagebus.MessageBus;
import com.devicehive.websockets.messagebus.NotificationsSubscriptionManager;
import com.devicehive.websockets.util.SessionUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.websocket.Session;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Singleton
public class DeviceMessageHandlers implements HiveMessageHandlers {

    private static final Logger logger = LoggerFactory.getLogger(DeviceMessageHandlers.class);

    @Inject
    private MessageBus messageBus;

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
        DeviceCommand oldCommand = null;//TODO get from DB
        DeviceCommand deviceCommand = GsonFactory.createGson().fromJson(message.getAsJsonObject("command"), DeviceCommand.class);
        oldCommand.setCommand(deviceCommand.getCommand());
        oldCommand.setParameters(deviceCommand.getParameters());
        oldCommand.setLifetime(deviceCommand.getLifetime());
        oldCommand.setFlags(deviceCommand.getFlags());
        oldCommand.setStatus(deviceCommand.getStatus());
        oldCommand.setResult(deviceCommand.getResult());
        //TODO save oldCommand to DB


        return JsonMessageFactory.createSuccessResponse();
    }

    @Action(value = "command/subscribe")
    public JsonObject processNotificationSubscribe(JsonObject message, Session session) {
        Gson gson = GsonFactory.createGson();
        Date timestamp = gson.fromJson(message.getAsJsonPrimitive("timestamp"), Date.class);
        timestamp = timestamp != null ? timestamp : new Date();
        UUID deviceId = gson.fromJson(message.getAsJsonPrimitive("deviceId"), UUID.class);

        synchronized (session) {
            messageBus.subscribeToCommands(deviceId, session);
            List<DeviceCommand> oldCommands = new ArrayList<DeviceCommand>();//TODO get non-delivered commands from DB
            for (DeviceCommand dc : oldCommands) {
                SessionUtil.sendCommand(dc, session);
                //TODO mark dc as delivered
            }
        }
        return JsonMessageFactory.createSuccessResponse();
    }

    @Action(value = "command/unsubscribe")
    public JsonObject processNotificationUnsubscribe(JsonObject message, Session session) {
        UUID deviceId = GsonFactory.createGson().fromJson(message.getAsJsonPrimitive("deviceId"), UUID.class);
        messageBus.unsubscribeFromCommands(deviceId, session);
        return JsonMessageFactory.createSuccessResponse();
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
