package com.devicehive.websockets.handlers;

import com.devicehive.configuration.Constants;
import com.devicehive.dao.ConfigurationDAO;
import com.devicehive.dao.DeviceCommandDAO;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.GsonFactory;
import com.devicehive.json.adapters.DateAdapter;
import com.devicehive.messages.MessageDetails;
import com.devicehive.messages.MessageType;
import com.devicehive.messages.bus.MessageBus;
import com.devicehive.model.*;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.service.DeviceService;
import com.devicehive.websockets.handlers.annotations.Action;
import com.devicehive.websockets.util.AsyncMessageDeliverer;
import com.devicehive.websockets.util.WebsocketSession;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.websocket.Session;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.List;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class DeviceMessageHandlers implements HiveMessageHandlers {

    private static final Logger logger = LoggerFactory.getLogger(DeviceMessageHandlers.class);
    @Inject
    private MessageBus messageBus;
    @Inject
    private DeviceDAO deviceDAO;
    @Inject
    private DeviceCommandDAO deviceCommandDAO;
    @Inject
    private DeviceService deviceService;
    @Inject
    private ConfigurationDAO configurationDAO;
    @Inject
    private AsyncMessageDeliverer asyncMessageDeliverer;

    @Action(value = "authenticate", needsAuth = false)
    public JsonObject processAuthenticate(JsonObject message, Session session) {
        UUID deviceId = GsonFactory.createGson().fromJson(message.get("deviceId"), UUID.class);
        if (deviceId == null || message.get("deviceKey") == null) {
            throw new HiveException("Device authentication error: credentials are incorrect");
        }
        String deviceKey = message.get("deviceKey").getAsString();
        logger.debug("authenticate action for " + deviceId);
        Device device = deviceDAO.findByUUIDAndKey(deviceId, deviceKey);

        if (device != null) {
            WebsocketSession.setAuthorisedDevice(session, device);
            return JsonMessageBuilder.createSuccessResponseBuilder().build();
        } else {
            throw new HiveException("Device authentication error: credentials are incorrect");
        }
    }

    @Override
    public void ensureAuthorised(JsonObject request, Session session) {
        Gson gson = GsonFactory.createGson();

        if (WebsocketSession.hasAuthorisedDevice(session)) {
            return;
        }
        UUID deviceId = gson.fromJson(request.get("deviceId"), UUID.class);
        if (request.get("deviceKey") == null) {
            throw new HiveException("device key cannot be empty");
        }
        String deviceKey = request.get("deviceKey").getAsString();

        Device device = deviceDAO.findByUUIDAndKey(deviceId, deviceKey);
        if (device == null) {
            throw new HiveException("Not authorised");
        }
    }

    @Action(value = "command/update")
    public JsonObject processCommandUpdate(JsonObject message, Session session) throws JMSException {
        logger.debug("command update action started for session : " + session.getId());
        DeviceCommand update = GsonFactory.createGson(COMMAND_UPDATE_FROM_DEVICE)
                .fromJson(message.getAsJsonObject("command"), DeviceCommand.class);
        if (message.get("commandId") == null) {
            throw new HiveException("Device command identifier cannot be null");
        }
        if (update == null) {
            throw new HiveException("DeviceCommand resource cannot be null");
        }
        update.setId(GsonFactory.createGson().fromJson(message.get("commandId"), Long.class));
        Device device = getDevice(session, message);

        logger.debug("submit device command update for device : " + device.getId());
        deviceService.submitDeviceCommandUpdate(update, device);

        logger.debug("command update action finished for session : " + session.getId());
       
        return JsonMessageBuilder.createSuccessResponseBuilder().build();
    }

    @Action(value = "command/subscribe")
    public JsonObject processCommandSubscribe(JsonObject message, Session session) throws IOException {
        logger.debug("command subscribe action started for session : " + session.getId());
        Gson gson = GsonFactory.createGson();
        Device device = getDevice(session, message);
        Date timestamp;
        try {
            timestamp = gson.fromJson(message.get(JsonMessageBuilder.TIMESTAMP), Date.class);
        } catch (JsonParseException e) {
            throw new HiveException(e.getCause().getMessage() + " Date must be in format " + DateAdapter.UTC_DATE_FORMAT_PATTERN, e);
        }
        if (timestamp == null) {
            timestamp = new Date();
        }
        try {
            WebsocketSession.getCommandsSubscriptionsLock(session).lock();
            logger.debug("will subscribe device for commands : " + device.getGuid());
            messageBus.subscribe(MessageType.CLIENT_TO_DEVICE_COMMAND,
                    MessageDetails.create().ids(device.getId()).session(session.getId()));
            logger.debug("will get commands newer than : " + timestamp);
            List<DeviceCommand> commandsFromDatabase = deviceCommandDAO.getNewerThan(device, timestamp);
            for (DeviceCommand deviceCommand : commandsFromDatabase) {
                logger.debug("will add command to queue : " + deviceCommand.getId());
                WebsocketSession
                        .addMessagesToQueue(session, ServerResponsesFactory.createCommandInsertMessage(deviceCommand));
            }
        } finally {
            WebsocketSession.getCommandsSubscriptionsLock(session).unlock();
        }
        logger.debug("deliver messages for session " + session.getId());
        asyncMessageDeliverer.deliverMessages(session);
        logger.debug("command subscribe ended for session : " + session.getId());
        return JsonMessageBuilder.createSuccessResponseBuilder().build();
    }

    @Action(value = "command/unsubscribe")
    public JsonObject processNotificationUnsubscribe(JsonObject message, Session session) {
        Device device = getDevice(session, message);
        logger.debug("command/unsubscribe for device" + device.getGuid());
        messageBus.unsubscribe(MessageType.CLIENT_TO_DEVICE_COMMAND,
                MessageDetails.create().ids(device.getId()).session(session.getId()));
        return JsonMessageBuilder.createSuccessResponseBuilder().build();
    }

    @Action(value = "notification/insert")
    public JsonObject processNotificationInsert(JsonObject message, Session session) throws JMSException {
        logger.debug("notification/insert started for session " + session.getId());
        DeviceNotification deviceNotification = GsonFactory.createGson(NOTIFICATION_FROM_DEVICE)
                .fromJson(message.get("notification"), DeviceNotification.class);
        if (deviceNotification == null || deviceNotification.getNotification() == null) {
            throw new HiveException("Notification is empty!");
        }
        Device device = getDevice(session, message);
        logger.debug("process submit device notification started for deviceNotification : " + deviceNotification
                .getNotification() + " and device : " + device.getGuid());
        deviceService.submitDeviceNotification(deviceNotification, device, session);
        JsonObject jsonObject = JsonMessageBuilder.createSuccessResponseBuilder().build();
        jsonObject.add("notification", GsonFactory.createGson(NOTIFICATION_TO_DEVICE).toJsonTree(deviceNotification));
        logger.debug("notification/insert ended for session " + session.getId());
        
        return jsonObject;
    }

    @Action(value = "server/info", needsAuth = false)
    public JsonObject processServerInfo(JsonObject message, Session session) {
        logger.debug("server/info action started. Session " + session.getId());
        Gson gson = GsonFactory.createGson(WEBSOCKET_SERVER_INFO);
        ApiInfo apiInfo = new ApiInfo();
        apiInfo.setApiVersion(Version.VERSION);
        apiInfo.setServerTimestamp(new Timestamp(System.currentTimeMillis()));
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

    @Action(value = "device/get")
    public JsonObject processDeviceGet(JsonObject message, Session session) {
        Device device = getDevice(session, message);
        Gson gsonResponse = GsonFactory.createGson(DEVICE_PUBLISHED);
        JsonElement deviceElem = gsonResponse.toJsonTree(device);
        JsonObject result = JsonMessageBuilder.createSuccessResponseBuilder()
                .addElement("device", deviceElem)
                .build();
        return result;
    }

    @Action(value = "device/save", needsAuth = false)
    public JsonObject processDeviceSave(JsonObject message, Session session) {
        logger.debug("device/save process started for session" + session.getId());
        UUID deviceId = GsonFactory.createGson().fromJson(message.get("deviceId"), UUID.class);
        if (deviceId == null) {
            throw new HiveException("Device ID is undefined!");
        }
        if (message.get("deviceKey") == null) {
            throw new HiveException("Device key is undefined!");
        }
        Gson mainGson = GsonFactory.createGson(DEVICE_SUBMITTED);
        DeviceUpdate device = mainGson.fromJson(message.get("device"), DeviceUpdate.class);
        logger.debug("check requered fields in device ");
        deviceService.checkDevice(device);
        Gson gsonForEquipment = GsonFactory.createGson();
        boolean useExistingEquipment = message.getAsJsonObject("device").get("equipment") == null;
        Set<Equipment> equipmentSet = gsonForEquipment.fromJson(message.getAsJsonObject("device").get("equipment"),
                new TypeToken<HashSet<Equipment>>() {
                }.getType());
        if (equipmentSet != null) {
            equipmentSet.remove(null);
        }
        logger.debug("device/save started");

        NullableWrapper<UUID> uuidNullableWrapper = new NullableWrapper<>();
        uuidNullableWrapper.setValue(deviceId);

        device.setGuid(uuidNullableWrapper);
        deviceService.deviceSave(device, equipmentSet, useExistingEquipment);
        JsonObject jsonResponseObject = JsonMessageBuilder.createSuccessResponseBuilder()
                .addAction("device/save")
                .addRequestId(message.get("requestId"))
                .build();
        logger.debug("device/save process ended for session" + session.getId());
        return jsonResponseObject;
    }

    private Device getDevice(Session session, JsonObject request) {
        if (WebsocketSession.hasAuthorisedDevice(session)) {
            return WebsocketSession.getAuthorisedDevice(session);
        }
        Gson gson = GsonFactory.createGson();
        UUID deviceId = gson.fromJson(request.get("deviceId"), UUID.class);
        return deviceDAO.findByUUID(deviceId);
    }


}
