package com.devicehive.websockets.handlers;


import com.devicehive.dao.*;
import com.devicehive.exceptions.HiveException;
import com.devicehive.exceptions.HiveWebsocketException;
import com.devicehive.model.*;
import com.devicehive.service.DeviceService;
import com.devicehive.websockets.handlers.annotations.Action;
import com.devicehive.websockets.json.GsonFactory;
import com.devicehive.websockets.json.strategies.*;
import com.devicehive.websockets.messagebus.global.MessagePublisher;
import com.devicehive.websockets.messagebus.local.LocalMessageBus;
import com.devicehive.websockets.util.WebsocketSession;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.transaction.Transactional;
import javax.websocket.Session;
import java.util.*;

public class DeviceMessageHandlers implements HiveMessageHandlers {

    private static final Logger logger = LoggerFactory.getLogger(DeviceMessageHandlers.class);
    @Inject
    private LocalMessageBus localMessageBus;
    @Inject
    private MessagePublisher messagePublisher;
    @Inject
    private DeviceDAO deviceDAO;
    @Inject
    private DeviceClassDAO deviceClassDAO;
    @Inject
    private DeviceCommandDAO deviceCommandDAO;
    @Inject
    private DeviceService deviceService;
    @Inject
    private NetworkDAO networkDAO;
    @Inject
    private EquipmentDAO equipmentDAO;

    @Action(value = "authenticate", needsAuth = false)
    public JsonObject processAuthenticate(JsonObject message, Session session) {
        UUID deviceId = GsonFactory.createGson().fromJson(message.get("deviceId"), UUID.class);
        String deviceKey = message.get("deviceKey").getAsString();

        Device device = deviceDAO.findByUUIDAndKey(deviceId, deviceKey);

        if (device != null) {
            WebsocketSession.setAuthorisedDevice(session, device);
            return JsonMessageBuilder.createSuccessResponseBuilder().build();
        } else {
            throw new HiveWebsocketException("Device authentication error: credentials are incorrect");
        }
    }

    @Override
    public void ensureAuthorised(JsonObject request, Session session) {
        Gson gson = GsonFactory.createGson();

        if (WebsocketSession.hasAuthorisedDevice(session)) {
            return;
        }
        UUID deviceId = gson.fromJson(request.get("deviceId"), UUID.class);
        String deviceKey = request.get("deviceKey").getAsString();

        Device device = deviceDAO.findByUUIDAndKey(deviceId, deviceKey);
        if (device == null) {
            throw new HiveWebsocketException("Not authorised");
        }
    }

    @Action(value = "command/update")
    public JsonObject processCommandUpdate(JsonObject message, Session session) throws JMSException {
        DeviceCommand command = deviceCommandDAO.findById(message.get("commandId").getAsLong());
        DeviceCommand update = GsonFactory.createGson(new CommandUpdateExclusionStrategy())
            .fromJson(message.getAsJsonObject("command"), DeviceCommand.class);
        Device device = getDevice(session, message);

        //deviceService.submitDeviceCommandUpdate(update, device);

        return JsonMessageBuilder.createSuccessResponseBuilder().build();
    }

    @Action(value = "command/subscribe")
    public JsonObject processNotificationSubscribe(JsonObject message, Session session) {
        Gson gson = GsonFactory.createGson();
        Date timestamp = gson.fromJson(message.getAsJsonPrimitive("timestamp"), Date.class);

        Device device = getDevice(session, message);

        if (timestamp != null) {
            try {
                WebsocketSession.getCommandsSubscriptionsLock(session).lock();
                localMessageBus.subscribeForCommands(device, session);
                List<DeviceCommand> oldCommands = deviceCommandDAO.getNewerThan(device, timestamp);
                gson = GsonFactory.createGson(new DeviceCommandInsertExclusionStrategy());
                for (DeviceCommand deviceCommand : oldCommands) {
                    WebsocketSession.deliverMessages(session, gson.toJsonTree(deviceCommand, DeviceCommand.class));
                }
            } finally {
                WebsocketSession.getCommandsSubscriptionsLock(session).unlock();
            }
        } else {
            localMessageBus.subscribeForCommands(device, session);
        }
        return JsonMessageBuilder.createSuccessResponseBuilder().build();
    }

    @Action(value = "command/unsubscribe")
    public JsonObject processNotificationUnsubscribe(JsonObject message, Session session) {
        Device device = getDevice(session, message);
        localMessageBus.unsubscribeFromCommands(device, session);
        return JsonMessageBuilder.createSuccessResponseBuilder().build();
    }

    @Action(value = "notification/insert")
    public JsonObject processNotificationInsert(JsonObject message, Session session) throws JMSException {

        DeviceNotification deviceNotification = GsonFactory.createGson(new NotificationInsertRequestExclusionStrategy())
                .fromJson(message.get("notification"), DeviceNotification.class);

        Device device = getDevice(session, message);

        deviceService.submitDeviceNotification(deviceNotification, device);

        JsonObject jsonObject = JsonMessageBuilder.createSuccessResponseBuilder()
            .addElement("notification", new JsonObject())
            .build();
        return jsonObject;
    }

    @Action(value = "server/info")
    @Transactional
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

    @Action(value = "device/get")
    public JsonObject processDeviceGet(JsonObject message, Session session) {
        Gson gson =  GsonFactory.createGson();
        JsonElement requestId = message.get("requestId");
        UUID deviceId = GsonFactory.createGson().fromJson(message.get("deviceId"),
                UUID.class);
        Device device = deviceDAO.findByUUID(deviceId);

        Gson gsonResponse = GsonFactory.createGson(new DeviceGetExclusionStrategy());
        JsonElement deviceElem = gsonResponse.toJsonTree(device);
        JsonObject result = JsonMessageBuilder.createSuccessResponseBuilder()
                .addRequestId(requestId)
                .addElement("device", deviceElem)
                .build();
        return result;
    }

    @Action(value = "device/save", needsAuth = false)
    @Transactional
    public JsonObject processDeviceSave(JsonObject message, Session session) {
        UUID deviceId = GsonFactory.createGson().fromJson(message.get("deviceId"), UUID.class);
        if (deviceId == null) {
            throw new HiveWebsocketException("Device ID is empty");
        }
        String deviceKey = message.get("deviceKey").getAsString();
        if (deviceKey == null) {
            throw new HiveWebsocketException("Device key is empty");
        }
        Gson mainGson = GsonFactory.createGson(new DeviceSaveExclusionStrategy());
        Device device = mainGson.fromJson(message.get("device"), Device.class);
        checkDevice(device);
        Gson gsonForEquipment = GsonFactory.createGson();
        Set<Equipment> equipmentSet = gsonForEquipment.fromJson(message.getAsJsonObject("device").get("equipment"),
                new TypeToken<HashSet<Equipment>>() {
                }.getType());
        if (equipmentSet != null) {
            equipmentSet.remove(null);
        }
        Device existingDevice = deviceDAO.findByUUID(deviceId);
        try {
            if (existingDevice != null) {
                device.setId(existingDevice.getId());
                device.setGuid(deviceId);
                deviceService.updateDevice(device, equipmentSet);
            } else {
                device.setGuid(deviceId);
                deviceService.registerDevice(device, equipmentSet);
            }
        } catch (HiveException e) {
            throw new HiveWebsocketException(e.getMessage(), e);
        }
        JsonObject jsonResponseObject = JsonMessageBuilder.createSuccessResponseBuilder()
                .addAction("device/save")
                .addRequestId(message.get("requestId"))
                .build();
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

    private void checkDevice(Device device) throws HiveWebsocketException {
        if (device == null) {
            throw new HiveWebsocketException("Device is empty");
        }
        if (device.getName() == null) {
            throw new HiveWebsocketException("Device name is empty");
        }
        if (device.getKey() == null) {
            throw new HiveWebsocketException("Device key is empty");
        }
        if (device.getDeviceClass() == null) {
            throw new HiveWebsocketException("Device class is empty");
        }
    }

}
