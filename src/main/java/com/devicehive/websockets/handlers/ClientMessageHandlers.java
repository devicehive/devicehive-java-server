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
import com.devicehive.websockets.util.WebsocketThreadPoolSingleton;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.websocket.Session;
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
    @Inject
    private ConfigurationDAO configurationDAO;
    @Inject
    private DeviceNotificationService deviceNotificationService;
    @Inject
    private WebsocketThreadPoolSingleton threadPoolSingleton;

    @Action(value = "authenticate", needsAuth = false)
    public JsonObject processAuthenticate(JsonObject message, Session session) {

        String login = message.get("login").getAsString();
        String password = message.get("password").getAsString();
        logger.debug("authenticate action for " + login);
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
        logger.debug("command/insert action for " + deviceGuid + ". Session " + session.getId());
        if (deviceGuid == null) {
            throw new HiveException("Device ID is empty");
        }

        User user = WebsocketSession.getAuthorisedUser(session);
        Device device;
        if (user.getRole() == User.ROLE.Administrator.ordinal()) {
            device = deviceDAO.findByUUID(deviceGuid);
        } else {
            device = deviceDAO.findByUUIDAndUser(user, deviceGuid);
        }

        logger.debug("command/insert action for " + deviceGuid + ". Device found: " + device + ". Session " +
                session.getId());
        if (device == null) {
            throw new HiveException("Unknown Device ID");
        }

        DeviceCommand deviceCommand = gson.fromJson(message.getAsJsonObject("command"), DeviceCommand.class);
        logger.debug(
                "command/insert action for " + deviceGuid + ". Device command found: " + deviceCommand + ". Session " +
                        session.getId());
        if (deviceCommand == null) {
            throw new HiveException("Command is empty");
        }

        logger.debug("submit device command process" + ". Session " + session.getId());
        deviceService
                .submitDeviceCommand(deviceCommand, device, user, session); //saves command to DB and sends it in JMS
        deviceCommand.setUser(user);
        JsonObject jsonObject = JsonMessageBuilder.createSuccessResponseBuilder()
                .addElement("command", GsonFactory.createGson(new ClientCommandInsertResponseExclusionStrategy())
                        .toJsonTree(deviceCommand))
                .build();
        logger.debug("submit device command ended" + ". Session " + session.getId());
        return jsonObject;
    }

    @Action(value = "notification/subscribe")
    public JsonObject processNotificationSubscribe(JsonObject message, Session session) {
        logger.debug("notification/subscribe action" + ". Session " + session.getId());
        Gson gson = GsonFactory.createGson();
        Date timestamp = gson.fromJson(message.get(JsonMessageBuilder.TIMESTAMP), Date.class);
        if (timestamp == null) {
            timestamp = new Date(System.currentTimeMillis());
        }
        //TODO set notification's limit (do not try to get notifications for last year :))
        List<UUID> list = gson.fromJson(message.get(JsonMessageBuilder.DEVICE_GUIDS), new TypeToken<List<UUID>>() {
        }.getType());

        if (list == null || list.isEmpty()) {
            processNotificationSubscribeNullCase(session, timestamp, gson);
        } else {
            processNotificationSubscribeNotNullCase(list, session, timestamp, gson);
        }


        JsonObject jsonObject = JsonMessageBuilder.createSuccessResponseBuilder().build();
        logger.debug("notification/subscribe action  finished");
        return jsonObject;

    }

    private void processNotificationSubscribeNullCase(Session session, Date timestamp, Gson gson) {
        logger.debug("notification/subscribe action - null guid case." + ". Session " + session.getId());
        User authorizedUser = WebsocketSession.getAuthorisedUser(session);
        List<DeviceNotification> deviceNotifications;
        if (authorizedUser.getRole() == User.ROLE.Administrator.ordinal()) {
            deviceNotifications =
                    deviceNotificationService.getDeviceNotificationList(null, authorizedUser, timestamp, true);
        } else {
            deviceNotifications =
                    deviceNotificationService.getDeviceNotificationList(null, authorizedUser, timestamp, false);
        }

        if (deviceNotifications == null) {
            logger.debug("notification/subscribe action - null guid case." + "no device notifications found " +
                    "Session " + session.getId());
            return;
        }
        logger.debug("notification/subscribe action - null guid case." + "get device notification. found " +
                deviceNotifications.size() + " notifications. " + "Session " + session.getId());
        try {
            WebsocketSession.getNotificationSubscriptionsLock(session).lock();
            logger.debug("process notification subscribe for session " + session.getId());
            localMessageBus.subscribeForNotifications(session.getId(), null);
            if (!deviceNotifications.isEmpty()) {
                for (DeviceNotification deviceNotification : deviceNotifications) {
                    logger.debug("This device notification will be added to queue: " + deviceNotification +
                            "Session " + session.getId());
                    WebsocketSession.addMessagesToQueue(session, gson.toJsonTree(deviceNotification,
                            DeviceNotification.class));
                }
            }
        } finally {
            WebsocketSession.getNotificationSubscriptionsLock(session).unlock();
            logger.debug("deliver messages process for session" + session.getId());
            threadPoolSingleton.deliverMessagesAndNotify(session);
        }

    }

    private void processNotificationSubscribeNotNullCase(List<UUID> guids, Session session,
                                                         Date timestamp, Gson gson) {
        logger.debug("notification/subscribe action - null guid case." + ". Session " + session.getId());
        User authorizedUser = WebsocketSession.getAuthorisedUser(session);
        List<Device> devices;
        if (authorizedUser.getRole() == User.ROLE.Administrator.ordinal()) {
            devices = deviceDAO.findByUUID(guids);
        } else {
            devices = deviceDAO.findByUUIDListAndUser(authorizedUser, guids);
        }
        if (devices == null) {
            logger.debug("No devices found. Return " + ". Session " + session.getId());
            return;
        }
        logger.debug("Found " + devices.size() + " devices" + ". Session " + session.getId());
        List<DeviceNotification> deviceNotifications = deviceNotificationService.getDeviceNotificationList(devices,
                authorizedUser, timestamp, null);
        if (deviceNotifications == null) {
            logger.debug("notification/subscribe action - not null guid case." + "no device notifications found " +
                    "Session " + session.getId());
            return;
        }
        try {
            logger.debug("notification/subscribe action - not null guid case. found " + deviceNotifications.size() +
                    " devices. " + "Session " + session.getId());
            WebsocketSession.getNotificationSubscriptionsLock(session).lock();
            localMessageBus.subscribeForNotifications(session.getId(), devices);
            if (deviceNotifications != null && !deviceNotifications.isEmpty()) {
                for (DeviceNotification deviceNotification : deviceNotifications) {
                    logger.debug("This device notification will be added to queue: " + deviceNotification +
                            "Session " + session.getId());
                    WebsocketSession.addMessagesToQueue(session, gson.toJsonTree(deviceNotification,
                            DeviceNotification.class));
                }
            }
        } finally {
            WebsocketSession.getNotificationSubscriptionsLock(session).unlock();
            logger.debug("deliver messages process for session" + session.getId());
            threadPoolSingleton.deliverMessagesAndNotify(session);
        }
    }

    @Action(value = "notification/unsubscribe")
    public JsonObject processNotificationUnsubscribe(JsonObject message, Session session) {
        logger.debug("notification/unsubscribe action. Session " + session.getId());
        Gson gson = GsonFactory.createGson();
        List<UUID> list = gson.fromJson(message.get(JsonMessageBuilder.DEVICE_GUIDS), new TypeToken<List<UUID>>() {
        }.getType());
        try {
            WebsocketSession.getNotificationSubscriptionsLock(session).lock();
            List<Device> devices = null;
            if (list != null && !list.isEmpty()) {
                devices = deviceDAO.findByUUID(list);

            }
            logger.debug("notification/unsubscribe. found " + devices.size() +
                    " devices. " + "Session " + session.getId());
            logger.debug("notification/unsubscribe. performing unsubscribing action");
            localMessageBus.unsubscribeFromNotifications(session.getId(), devices);
        } finally {
            WebsocketSession.getNotificationSubscriptionsLock(session).unlock();
        }
        JsonObject jsonObject = JsonMessageBuilder.createSuccessResponseBuilder().build();
        logger.debug("notification/unsubscribe completed for session " + session.getId());
        return jsonObject;
    }

    @Action(value = "server/info", needsAuth = false)
    public JsonObject processServerInfo(JsonObject message, Session session) {
        logger.debug("server/info action started. Session " + session.getId());
        Gson gson = GsonFactory.createGson(new ServerInfoExclusionStrategy());
        ApiInfo apiInfo = new ApiInfo();
        apiInfo.setApiVersion(Version.VERSION);
        apiInfo.setServerTimestamp(new Date(System.currentTimeMillis()));
        Configuration webSocketServerUrl = configurationDAO.findByName(Constants.WEBSOCKET_SERVER_URL);
        if (webSocketServerUrl == null) {
            logger.error("Websocket server url isn't set!");
            throw new HiveException("Websocket server url isn't set!");
        }
        apiInfo.setWebSocketServerUrl(webSocketServerUrl.getValue());
        JsonObject jsonObject = JsonMessageBuilder.createSuccessResponseBuilder()
                .addElement("info", gson.toJsonTree(apiInfo))
                .build();
        logger.debug("server/info action completed. Session " + session.getId());
        return jsonObject;
    }

    @Action(value = "configuration/set")
    public JsonObject processConfigurationSet(JsonObject message, Session session) {
        logger.debug("configuration/set action started. Session " + session.getId());
        User user = WebsocketSession.getAuthorisedUser(session);
        Integer roleAdmin = User.ROLE.Administrator.ordinal();
        if (user.getRole() == null || !user.getRole().equals(roleAdmin)) {
            throw new HiveException("No permissions");
        }
        Gson gson = GsonFactory.createGson();

        Configuration configuration =
                new Configuration(message.get("name").getAsString(), message.get("value").getAsString());
        if (configurationDAO.findByName(configuration.getName()) == null) {
            logger.debug("save configuration. Session " + session.getId());
            configurationDAO.saveConfiguration(configuration);
        } else {
            logger.debug("merge configuration. Session " + session.getId());
            configurationDAO.updateConfiguration(configuration);
        }
        JsonObject response = JsonMessageBuilder.createSuccessResponseBuilder()
                .addElement("requestId", message.get("requestId"))
                .build();
        logger.debug("configuration/set action completed. Session " + session.getId());
        return response;
    }

}
