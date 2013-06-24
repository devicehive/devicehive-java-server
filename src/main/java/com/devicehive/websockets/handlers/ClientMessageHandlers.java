package com.devicehive.websockets.handlers;


import com.devicehive.dao.DeviceDAO;
import com.devicehive.exceptions.HiveWebsocketException;
import com.devicehive.model.*;
import com.devicehive.service.DeviceService;
import com.devicehive.service.UserService;
import com.devicehive.websockets.handlers.annotations.Action;
import com.devicehive.websockets.json.GsonFactory;
import com.devicehive.websockets.json.strategies.ClientCommandInsertRequestExclusionStrategy;
import com.devicehive.websockets.json.strategies.ClientCommandInsertResponseExclusionStrategy;
import com.devicehive.websockets.json.strategies.ServerInfoExclusionStrategy;
import com.devicehive.websockets.messagebus.global.MessagePublisher;
import com.devicehive.websockets.messagebus.local.LocalMessageBus;
import com.devicehive.websockets.util.WebsocketSession;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.transaction.Transactional;
import javax.websocket.Session;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ClientMessageHandlers implements HiveMessageHandlers {

    private static final Logger logger = LoggerFactory.getLogger(ClientMessageHandlers.class);

    @Inject
    private MessagePublisher messagePublisher;

    @Inject
    private LocalMessageBus localMessageBus;

    @Inject
    private UserService userService;

    @Inject
    private DeviceService deviceService;

    @Inject
    private DeviceDAO deviceDAO;





    @Action(value = "authenticate", needsAuth = false)
    //@Transactional
    public JsonObject processAuthenticate(JsonObject message, Session session) {
        String login = message.get("login").getAsString();
        String password = message.get("password").getAsString();

        User user = userService.authenticate(login, password);

        if (user != null) {
            WebsocketSession.setAuthorisedUser(session, user);
            return JsonMessageBuilder.createSuccessResponseBuilder().build();
        } else {
            throw new HiveWebsocketException("Client authentication error: credentials are incorrect");
        }
    }

    @Override
    public void ensureAuthorised(JsonObject request, Session session) {
        if (WebsocketSession.hasAuthorisedUser(session)) {
            throw new HiveWebsocketException("Not authorised");
        }
    }

    @Action(value = "command/insert")
    public JsonObject processCommandInsert(JsonObject message, Session session) throws JMSException { //TODO?!
        Gson gson = GsonFactory.createGson(new ClientCommandInsertRequestExclusionStrategy());

        UUID deviceGuid = gson.fromJson(message.get(JsonMessageBuilder.DEVICE_GUID), UUID.class);

        if (deviceGuid == null) {
            throw new HiveWebsocketException("Device ID is empty");
        }

        Device device = deviceDAO.findByUUID(deviceGuid);
        if (device == null) {
            throw new HiveWebsocketException("Unknown Device ID");
        }

        DeviceCommand deviceCommand = gson.fromJson(message.getAsJsonObject("command"), DeviceCommand.class);


        if (deviceCommand == null && true /*TODO check network*/) {
            throw new HiveWebsocketException("Command is empty");
        }

        User user = WebsocketSession.getAuthorisedUser(session);
        deviceService.submitDeviceCommand(deviceCommand, device, user); //saves command to DB and sends it in JMS
        localMessageBus.subscribeForCommandUpdates(deviceCommand.getId(), session);


        JsonObject jsonObject = JsonMessageBuilder.createSuccessResponseBuilder()
            .addElement("command", GsonFactory.createGson(new ClientCommandInsertResponseExclusionStrategy()).toJsonTree(deviceCommand))
            .build();
        return jsonObject;
    }

    @Action(value = "notification/subscribe")
    public JsonObject processNotificationSubscribe(JsonObject message, Session session) {
        Gson gson = GsonFactory.createGson();

        Date timestamp = gson.fromJson(message.get(JsonMessageBuilder.TIMESTAMP), Date.class);//TODO


        JsonArray deviceGuidsJson = message.getAsJsonArray(JsonMessageBuilder.DEVICE_GUIDS);
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
        Gson gson = GsonFactory.createGson();
        JsonArray deviceGuidsJson = message.getAsJsonArray(JsonMessageBuilder.DEVICE_GUIDS);
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
        Gson gson = GsonFactory.createGson(new ServerInfoExclusionStrategy());
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
