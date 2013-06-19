package com.devicehive.websockets.handlers;




import com.devicehive.model.ApiInfo;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.Version;
import com.devicehive.websockets.handlers.annotations.Action;
import com.devicehive.websockets.json.GsonFactory;
import com.devicehive.websockets.json.strategies.client.CommandInsertRequestExclusionStrategy;
import com.devicehive.websockets.json.strategies.client.NotificationSubscribeRequestExclusionStrategy;
import com.devicehive.websockets.json.strategies.client.NotificationUnsubscribeRequestExclusionStrategy;
import com.devicehive.websockets.messagebus.global.MessagePublisher;
import com.devicehive.websockets.messagebus.local.LocalMessageBus;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.inject.Inject;
import javax.inject.Named;
import javax.jms.JMSException;
import javax.websocket.Session;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Named
public class ClientMessageHandlers implements HiveMessageHandlers {

    @Inject
    private MessagePublisher messagePublisher;

    @Inject
    private LocalMessageBus localMessageBus;


    @Action(value = "authenticate", needsAuth = false)
    public JsonObject processAuthenticate(JsonObject message, Session session) {
        String login = message.get("login").getAsString();
        String password = message.get("password").getAsString();

        return JsonMessageBuilder.createSuccessResponseBuilder().build();
    }


    @Action(value = "command/insert")
    public JsonObject processCommandInsert(JsonObject message, Session session) throws JMSException { //TODO?!
        Gson gson = GsonFactory.createGson(new CommandInsertRequestExclusionStrategy());
        UUID deviceGuid = gson.fromJson(message.get("deviceGuid"), UUID.class);
        DeviceCommand deviceCommand = gson.fromJson(message.getAsJsonObject("command"), DeviceCommand.class);
        DeviceCommand savedCommand = deviceCommand; //TODO save to DB

        messagePublisher.publishCommand(savedCommand);


        JsonObject jsonObject = JsonMessageBuilder.createSuccessResponseBuilder()
            .addElement("command", GsonFactory.createGson().toJsonTree(savedCommand))
            .build();
        return jsonObject;
    }

    @Action(value = "notification/subscribe")
    public JsonObject processNotificationSubscribe(JsonObject message, Session session) {
        Gson gson = GsonFactory.createGson(new NotificationSubscribeRequestExclusionStrategy());

        Date timestamp = gson.fromJson(message.getAsJsonPrimitive("timestamp"), Date.class);//TODO


        JsonArray  deviceGuidsJson = message.getAsJsonArray("deviceGuids");
        List<UUID> list = new ArrayList();
        for (JsonElement uuidJson : deviceGuidsJson) {
            list.add(gson.fromJson(uuidJson, UUID.class));
        }
        localMessageBus.subscribeForNotifications(session, list);
        JsonObject jsonObject = JsonMessageBuilder.createSuccessResponseBuilder().build();
        return jsonObject;

    }

    @Action(value = "notification/unsubscribe")
    public JsonObject processNotificationUnsubscribe(JsonObject message, Session session) {
        Gson gson = GsonFactory.createGson(new NotificationUnsubscribeRequestExclusionStrategy());
        JsonArray  deviceGuidsJson = message.getAsJsonArray("deviceGuids");
        List<UUID> list = new ArrayList();
        for (JsonElement uuidJson : deviceGuidsJson) {
            list.add(gson.fromJson(uuidJson, UUID.class));
        }
        localMessageBus.unsubscribeFromNotifications(session, list);
        JsonObject jsonObject = JsonMessageBuilder.createSuccessResponseBuilder()
            .addElement("deviceGuids", new JsonObject())
            .build();
        return jsonObject;
    }


    @Action(value = "server/info", needsAuth = false)
    public JsonObject processServerInfo(JsonObject message, Session session) {
        Gson gson = GsonFactory.createGson();
        ApiInfo apiInfo = new ApiInfo();
        apiInfo.setApiVersion(Version.VERSION);
        apiInfo.setServerTimestamp(new Date());
        apiInfo.setWebSocketServerUrl("TODO_URL");
        JsonObject jsonObject = JsonMessageBuilder.createSuccessResponseBuilder()
            .addElement("info", gson.toJsonTree(apiInfo))
            .build();
        return jsonObject;
    }
}
