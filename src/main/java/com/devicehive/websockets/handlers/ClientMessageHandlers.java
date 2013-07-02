package com.devicehive.websockets.handlers;


import com.devicehive.configuration.Constants;
import com.devicehive.dao.ConfigurationDAO;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.*;
import com.devicehive.service.DeviceNotificationService;
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
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.websocket.Session;
import java.util.Date;
import java.util.LinkedList;
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
    @Inject
    private ConfigurationDAO configurationDAO;
    @Inject
    private DeviceNotificationService deviceNotificationService;

    @Action(value = "authenticate", needsAuth = false)
    public JsonObject processAuthenticate(JsonObject message, Session session) {
        String login = message.get("login").getAsString();
        String password = message.get("password").getAsString();

        User user = userService.authenticate(login, password);

        if (user != null) {
            WebsocketSession.setAuthorisedUser(session, user);
            return JsonMessageBuilder.createSuccessResponseBuilder().build();
        } else {
            throw new HiveException("Client authentication error: credentials are incorrect");
        }
    }

    @Override
    public void ensureAuthorised(JsonObject request, Session session) {
        if (!WebsocketSession.hasAuthorisedUser(session)) {
            throw new HiveException("Not authorised");
        }
    }

    @Action(value = "command/insert")
    public JsonObject processCommandInsert(JsonObject message, Session session) {
        Gson gson = GsonFactory.createGson(new ClientCommandInsertRequestExclusionStrategy());

        UUID deviceGuid = gson.fromJson(message.get(JsonMessageBuilder.DEVICE_GUID), UUID.class);

        if (deviceGuid == null) {
            throw new HiveException("Device ID is empty");
        }

        Device device = deviceDAO.findByUUID(deviceGuid);
        if (device == null) {
            throw new HiveException("Unknown Device ID");
        }

        DeviceCommand deviceCommand = gson.fromJson(message.getAsJsonObject("command"), DeviceCommand.class);
        User user = WebsocketSession.getAuthorisedUser(session);

        if (deviceCommand == null) {
            throw new HiveException("Command is empty");
        }

        deviceService.submitDeviceCommand(deviceCommand, device, user, session); //saves command to DB and sends it in JMS
        deviceCommand.setUser(user);
        JsonObject jsonObject = JsonMessageBuilder.createSuccessResponseBuilder()
                .addElement("command", GsonFactory.createGson(new ClientCommandInsertResponseExclusionStrategy())
                        .toJsonTree(deviceCommand))
                .build();
        return jsonObject;
    }

    @Action(value = "notification/subscribe")
    @Transactional
    public JsonObject processNotificationSubscribe(JsonObject message, Session session) {
        Gson gson = GsonFactory.createGson();
        Date timestamp = gson.fromJson(message.get(JsonMessageBuilder.TIMESTAMP), Date.class);
        if (timestamp == null) {
            timestamp = new Date(System.currentTimeMillis());
        }
        //TODO set notification's limit (do not try to get notifications for last year :))
        List<UUID> list = gson.fromJson(message.get(JsonMessageBuilder.DEVICE_GUIDS), new TypeToken<List<UUID>>() {
        }.getType());
        List<Device> devices = new LinkedList<>();
        if (list != null && !list.isEmpty()) {
            devices = deviceDAO.findByUUIDAndUser(WebsocketSession.getAuthorisedUser(session), list);
        }
        try {
            WebsocketSession.getNotificationSubscriptionsLock(session).lock();
            localMessageBus.subscribeForNotifications(session.getId(), devices, timestamp);
            List<DeviceNotification> deviceNotificationList = deviceNotificationService.getDeviceNotificationList
                    (devices, timestamp);
            if (deviceNotificationList != null && !deviceNotificationList.isEmpty()) {
                for (DeviceNotification deviceNotification : deviceNotificationList) {
                    messagePublisher.publishNotification(deviceNotification);
                }
            }
        }finally {
            WebsocketSession.getNotificationSubscriptionsLock(session).unlock();
        }
        JsonObject jsonObject = JsonMessageBuilder.createSuccessResponseBuilder().build();
        return jsonObject;

    }

    @Action(value = "notification/unsubscribe")
    @Transactional
    public JsonObject processNotificationUnsubscribe(JsonObject message, Session session) {
        Gson gson = GsonFactory.createGson();
        List<UUID> list = gson.fromJson(message.get(JsonMessageBuilder.DEVICE_GUIDS), new TypeToken<List<UUID>>() {
        }.getType());
        try {
            WebsocketSession.getNotificationSubscriptionsLock(session).lock();
            List<Device> devices = new LinkedList<>();
            if (list != null && !list.isEmpty()) {
                devices = deviceDAO.findByUUID(list);

            }
            localMessageBus.unsubscribeFromNotifications(session.getId(), devices);
        } finally {
            WebsocketSession.getNotificationSubscriptionsLock(session).unlock();
        }
        JsonObject jsonObject = JsonMessageBuilder.createSuccessResponseBuilder().build();
        return jsonObject;
    }

    @Action(value = "server/info", needsAuth = false)
    public JsonObject processServerInfo(JsonObject message, Session session) {
        Gson gson = GsonFactory.createGson(new ServerInfoExclusionStrategy());
        ApiInfo apiInfo = new ApiInfo();
        apiInfo.setApiVersion(Version.VERSION);
        apiInfo.setServerTimestamp(new Date(System.currentTimeMillis()));
        Configuration webSocketServerUrl = configurationDAO.findByName(Constants.WEBSOCKET_SERVER_URL);
        apiInfo.setWebSocketServerUrl(webSocketServerUrl.getValue());
        JsonObject jsonObject = JsonMessageBuilder.createSuccessResponseBuilder()
                .addElement("info", gson.toJsonTree(apiInfo))
                .build();
        return jsonObject;
    }

    @Action(value = "configuration/set")
    public JsonObject processConfigurationSet(JsonObject message, Session session) {
        User user = WebsocketSession.getAuthorisedUser(session);
        Integer roleAdmin = User.ROLE.Administrator.ordinal();
        if (user.getRole() == null || !user.getRole().equals(roleAdmin)) {
            throw new HiveException("No permissions");
        }
        Gson gson = GsonFactory.createGson();

        Configuration configuration =
                new Configuration(message.get("name").getAsString(), message.get("value").getAsString());
        if (configurationDAO.findByName(configuration.getName()) == null) {
            configurationDAO.saveConfiguration(configuration);
        } else {
            configurationDAO.updateConfiguration(configuration);
        }
        JsonObject response = JsonMessageBuilder.createSuccessResponseBuilder()
                .addElement("requestId", message.get("requestId"))
                .build();
        return response;
    }

}
