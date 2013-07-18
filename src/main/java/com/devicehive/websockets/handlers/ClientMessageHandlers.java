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
import com.devicehive.websockets.messagebus.ServerResponsesFactory;
import com.devicehive.websockets.messagebus.global.MessagePublisher;
import com.devicehive.websockets.messagebus.local.LocalMessageBus;
import com.devicehive.websockets.util.AsyncMessageDeliverer;
import com.devicehive.websockets.util.WebsocketSession;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.websocket.Session;
import java.io.IOException;
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
    private AsyncMessageDeliverer asyncMessageDeliverer;

    @Action(value = "authenticate", needsAuth = false)
    public JsonObject processAuthenticate(JsonObject message, Session session) {
        if (message.get("login") == null || message.get("password") == null) {
            throw new HiveException("login and password cannot be emty!");
        }
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
    public JsonObject processNotificationSubscribe(JsonObject message, Session session) throws IOException {
        logger.debug("notification/subscribe action" + ". Session " + session.getId());
        Gson gson = GsonFactory.createGson();
        Date timestamp = gson.fromJson(message.get(JsonMessageBuilder.TIMESTAMP), Date.class);
        if (timestamp == null) {
            timestamp = new Date();
        }
        //TODO set notification's limit (do not try to get notifications for last year :))
        List<UUID> list = gson.fromJson(message.get(JsonMessageBuilder.DEVICE_GUIDS), new TypeToken<List<UUID>>() {
        }.getType());
        if (list == null || list.isEmpty()) {
            prepareForNotificationSubscribeNullCase(session, timestamp);
        } else {
            prepareForNotificationSubscribeNotNullCase(list, session, timestamp);
        }
        JsonObject jsonObject = JsonMessageBuilder.createSuccessResponseBuilder().build();
        logger.debug("notification/subscribe action  finished");
        return jsonObject;

    }

    private void prepareForNotificationSubscribeNullCase(Session session, Date timestamp) throws IOException {
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
        logger.debug("notification/subscribe action - null guid case." + "get device notification. found " +
                deviceNotifications.size() + " notifications. " + "Session " + session.getId());
        notificationSubscribeAction(deviceNotifications, session, null);
    }

    private void prepareForNotificationSubscribeNotNullCase(List<UUID> guids, Session session, Date timestamp)
            throws IOException {
        logger.debug("notification/subscribe action - null guid case." + ". Session " + session.getId());
        User authorizedUser = WebsocketSession.getAuthorisedUser(session);
        List<Device> devices;
        if (authorizedUser.getRole() == User.ROLE.Administrator.ordinal()) {
            devices = deviceDAO.findByUUID(guids);
        } else {
            devices = deviceDAO.findByUUIDListAndUser(authorizedUser, guids);
        }
        if (devices.isEmpty()) {
            logger.debug("No devices found. Return " + ". Session " + session.getId());
            throw new HiveException("No available devices found.");
        }

        logger.debug("Found " + devices.size() + " devices" + ". Session " + session.getId());
        List<DeviceNotification> deviceNotifications = deviceNotificationService.getDeviceNotificationList(devices,
                authorizedUser, timestamp, null);

        notificationSubscribeAction(deviceNotifications, session, devices);

        checkDevicesAndGuidsList(devices, guids, true);
    }

    private void notificationSubscribeAction(List<DeviceNotification> deviceNotifications, Session session,
                                             List<Device> devices)
            throws IOException {
        try {
            logger.debug("notification/subscribe action - not null guid case. found " + deviceNotifications.size() +
                    " devices. " + "Session " + session.getId());
            WebsocketSession.getNotificationSubscriptionsLock(session).lock();
            localMessageBus.subscribeForNotifications(session.getId(), devices);
            if (deviceNotifications != null && !deviceNotifications.isEmpty()) {
                for (DeviceNotification deviceNotification : deviceNotifications) {
                    logger.debug("This device notification will be added to queue: " + deviceNotification +
                            "Session " + session.getId());
                    WebsocketSession.addMessagesToQueue(session,
                            ServerResponsesFactory.createNotificationInsertMessage(deviceNotification));
                }
            }
        } finally {
            WebsocketSession.getNotificationSubscriptionsLock(session).unlock();
            logger.debug("deliver messages process for session" + session.getId());
            asyncMessageDeliverer.deliverMessages(session);
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
                logger.debug("notification/unsubscribe. found " + devices.size() +
                        " devices. " + "Session " + session.getId());
                if (devices.isEmpty()) {
                    throw new HiveException("No available devices found");
                }
            }
            logger.debug("notification/unsubscribe. performing unsubscribing action");
            localMessageBus.unsubscribeFromNotifications(session.getId(), devices);
            checkDevicesAndGuidsList(devices, list, false);
        } finally {
            WebsocketSession.getNotificationSubscriptionsLock(session).unlock();
        }
        JsonObject jsonObject = JsonMessageBuilder.createSuccessResponseBuilder().build();
        logger.debug("notification/unsubscribe completed for session " + session.getId());
        return jsonObject;
    }

    private void checkDevicesAndGuidsList(List<Device> devices, List<UUID> guids, boolean isSubscribe) {
        if (devices.size() != guids.size()) {
            StringBuilder responseBuilder;
            if (isSubscribe) {
                responseBuilder = new StringBuilder("Unable to subscribe for devices with guids: ");
            } else {
                responseBuilder = new StringBuilder("Unable to unsubscribe from devices with guids: ");
            }
            for (UUID guid : guids) {
                boolean contains = false;
                for (Device device : devices) {
                    if (device.getGuid().equals(guid)) {
                        contains = true;
                    }
                }
                if (!contains) {
                    responseBuilder.append(guid + " ");
                }
            }
            responseBuilder.append(". Device(s) with such guids doesn't exist(s) or you haven't permissions to get " +
                    "notifications from this device.");
            throw new HiveException(responseBuilder.toString());
        }
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
        if (message.get("name") == null || message.get("value") == null) {
            throw new HiveException("Name and value fields cannot be empty!");
        }
        Configuration configuration =
                new Configuration(message.get("name").getAsString(), message.get("value").getAsString());
        configurationDAO.mergeConfiguration(configuration);
        JsonObject response = JsonMessageBuilder.createSuccessResponseBuilder()
                .addElement("requestId", message.get("requestId"))
                .build();
        logger.debug("configuration/set action completed. Session " + session.getId());
        return response;
    }

}