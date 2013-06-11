package com.devicehive.websockets.handlers;




import com.devicehive.model.ApiInfo;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.Version;
import com.devicehive.websockets.handlers.annotations.Action;
import com.devicehive.websockets.json.GsonFactory;
import com.devicehive.websockets.subscriptions.NotificationsSubscriptionManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


import javax.inject.Inject;
import javax.inject.Singleton;
import javax.websocket.Session;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Singleton
public class ClientMessageHandlers implements HiveMessageHandlers {

    @Inject
    private NotificationsSubscriptionManager notificationsSubscriptionManager;


    @Action(value = "authenticate", needsAuth = false)
    public JsonObject processAuthenticate(JsonObject message, Session session) {
        String login = message.get("login").getAsString();
        String password = message.get("password").getAsString();

        return JsonMessageFactory.createSuccessResponse();
    }


    @Action(value = "command/insert")
    public JsonObject processCommandInsert(JsonObject message, Session session) {
        Gson gson = GsonFactory.createGson();
        UUID deviceGuid = gson.fromJson(message.get("deviceGuid"), UUID.class);
        DeviceCommand deviceCommand = gson.fromJson(message.getAsJsonObject("command"), DeviceCommand.class);


        DeviceCommand executedCommand = deviceCommand; //TODO execute
        JsonObject jsonObject = JsonMessageFactory.createSuccessResponse();
        jsonObject.add("command", GsonFactory.createGson().toJsonTree(executedCommand));
        return jsonObject;
    }

    @Action(value = "notification/subscribe")
    public JsonObject processNotificationSubscribe(JsonObject message, Session session) {
        Gson gson = GsonFactory.createGson();

        Date timestamp = gson.fromJson(message.getAsJsonPrimitive("timestamp"), Date.class);
        JsonArray  deviceGuidsJson = message.getAsJsonArray("deviceGuids");
        List<UUID> list = new ArrayList();
        for (JsonElement uuidJson : deviceGuidsJson) {
            list.add(gson.fromJson(uuidJson, UUID.class));
        }
        //TODO subscribe
        JsonObject jsonObject = JsonMessageFactory.createSuccessResponse();
        return jsonObject;

    }

    @Action(value = "notification/unsubscribe")
    public JsonObject processNotificationUnsubscribe(JsonObject message, Session session) {
        Gson gson = GsonFactory.createGson();
        JsonArray  deviceGuidsJson = message.getAsJsonArray("deviceGuids");
        List<UUID> list = new ArrayList();
        for (JsonElement uuidJson : deviceGuidsJson) {
            list.add(gson.fromJson(uuidJson, UUID.class));
        }
        //TODO unsubscribe
        JsonObject jsonObject = JsonMessageFactory.createSuccessResponse();
        jsonObject.add("deviceGuids", new JsonObject());
        return jsonObject;
    }


    @Action(value = "server/info", needsAuth = false)
    public JsonObject processServerInfo(JsonObject message, Session session) {
        Gson gson = GsonFactory.createGson();
        JsonObject jsonObject = JsonMessageFactory.createSuccessResponse();
        ApiInfo apiInfo = new ApiInfo();
        apiInfo.setApiVersion(Version.VERSION);
        apiInfo.setServerTimestamp(new Date());
        apiInfo.setWebSocketServerUrl("TODO_URL");
        jsonObject.add("info", gson.toJsonTree(apiInfo));
        return jsonObject;
    }
}
