package com.devicehive.websockets.handlers;

import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.GsonFactory;
import com.devicehive.messages.handler.WebsocketHandlerCreator;
import com.devicehive.messages.subscriptions.CommandSubscription;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.*;
import com.devicehive.model.updates.DeviceCommandUpdate;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.service.DeviceActivityService;
import com.devicehive.service.DeviceCommandService;
import com.devicehive.service.DeviceService;
import com.devicehive.service.TimestampService;
import com.devicehive.utils.LogExecutionTime;
import com.devicehive.websockets.handlers.annotations.Action;
import com.devicehive.websockets.util.AsyncMessageSupplier;
import com.devicehive.websockets.util.WebsocketSession;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.websocket.Session;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

@LogExecutionTime
public class DeviceMessageHandlers implements HiveMessageHandlers {

    private static final Logger logger = LoggerFactory.getLogger(DeviceMessageHandlers.class);
    @EJB
    private SubscriptionManager subscriptionManager;
    @EJB
    private DeviceDAO deviceDAO;
    @EJB
    private DeviceCommandService commandService;
    @EJB
    private DeviceService deviceService;
    @EJB
    private AsyncMessageSupplier asyncMessageDeliverer;
    @EJB
    private TimestampService timestampService;
    @EJB
    private ConfigurationService configurationService;
    @EJB
    private DeviceActivityService deviceActivityService;

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#WsReference/Device/authenticate">WebSocket API:
     * Device: authenticate</a>
     * Authenticates a device. After successful authentication, all subsequent messages may exclude deviceId and deviceKey parameters.
     *
     * @param message Json object with the following structure
     *                <pre>
     *                               {
     *                                 "action": {string},
     *                                 "requestId": {object},
     *                                 "deviceId": {guid},
     *                                 "deviceKey": {string}
     *                               }
     *                               </pre>
     * @param session Current session.
     * @return Json object with the following structure
     *         <pre>
     *                 {
     *                   "action": {string},
     *                   "status": {string},
     *                   "requestId": {object}
     *                 }
     *                 </pre>
     */
    @Action(value = "authenticate", needsAuth = false)
    public JsonObject processAuthenticate(JsonObject message, Session session) {
        UUID deviceId = GsonFactory.createGson().fromJson(message.get("deviceId"), UUID.class);
        if (deviceId == null || message.get("deviceKey") == null) {
            throw new HiveException("Device authentication error: credentials are incorrect");
        }
        String deviceKey = message.get("deviceKey").getAsString();
        logger.debug("authenticate action for " + deviceId);
        Device device = deviceService.authenticate(deviceId, deviceKey);

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

        Device device = deviceService.authenticate(deviceId, deviceKey);
        if (device == null) {
            throw new HiveException("Not authorised");
        }
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#WsReference/Device/commandupdate">WebSocket API:
     * Device: command/update</a>
     * Updates an existing device command.
     *
     * @param message Json object with the following structure:
     *                <pre>
     *                               {
     *                                 "action": {string},
     *                                 "requestId": {object},
     *                                 "deviceId": {guid},
     *                                 "deviceKey": {string},
     *                                 "commandId": {integer},
     *                                 "command": {
     *                                   "command": {string},
     *                                   "parameters": {object},
     *                                   "lifetime": {integer},
     *                                   "flags": {integer},
     *                                   "status": {string},
     *                                   "result": {object}
     *                                 }
     *                               }
     *                               </pre>
     * @param session Current session.
     * @return Json object with the following structure:
     *         <pre>
     *                 {
     *                   "action": {string},
     *                   "status": {string},
     *                   "requestId": {object}
     *                 }
     *                 </pre>
     */
    @Action(value = "command/update")
    public JsonObject processCommandUpdate(JsonObject message, Session session) {
        logger.debug("command update action started for session : {{}", session.getId());
        DeviceCommandUpdate update = GsonFactory.createGson(COMMAND_UPDATE_FROM_DEVICE)
                .fromJson(message.getAsJsonObject("command"), DeviceCommandUpdate.class);
        if (message.get("commandId") == null) {
            throw new HiveException("Device command identifier cannot be null");
        }
        if (update == null) {
            throw new HiveException("DeviceCommand resource cannot be null");
        }
        update.setId(GsonFactory.createGson().fromJson(message.get("commandId"), Long.class));
        Device device = getDevice(session, message);

        logger.debug("submit device command update for device : " + device.getId());
        commandService.submitDeviceCommandUpdate(update, device);

        logger.debug("command update action finished for session : " + session.getId());
        return JsonMessageBuilder.createSuccessResponseBuilder().build();
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#WsReference/Device/commandsubscribe">WebSocket API:
     * Device: command/subscribe</a>
     * Subscribes the device to commands. After subscription is completed, the server will start to send
     * command/insert messages to the connected device.
     *
     * @param message Json object with the following structure
     *                <pre>
     *                               {
     *                                 "action": {string},
     *                                 "requestId": {object},
     *                                 "deviceId": {guid},
     *                                 "deviceKey": {string},
     *                                 "timestamp": {datetime}
     *                               }
     *                               </pre>
     * @param session Current session
     * @return json object with the following structure:
     *         <pre>
     *                 {
     *                   "action": {string},
     *                   "status": {string},
     *                   "requestId": {object}
     *                 }
     *                 </pre>
     * @throws IOException if unable to deliver message
     */
    @Action(value = "command/subscribe")
    public JsonObject processCommandSubscribe(JsonObject message, Session session) throws IOException {
        logger.debug("command subscribe action started for session : " + session.getId());
        Gson gson = GsonFactory.createGson();
        Device device = getDevice(session, message);
        Timestamp timestamp;
        try {
            timestamp = gson.fromJson(message.get(JsonMessageBuilder.TIMESTAMP), Timestamp.class);
        } catch (JsonParseException e) {
            throw new HiveException(e.getCause().getMessage() + " Incorrect timestamp format", e);
        }
        if (timestamp == null) {
            timestamp = timestampService.getTimestamp();
        }
        try {
            WebsocketSession.getCommandsSubscriptionsLock(session).lock();
            logger.debug("will subscribe device for commands : " + device.getGuid());

            CommandSubscription commandSubscription = new CommandSubscription(device.getId(), session.getId(),
                    new WebsocketHandlerCreator(session, WebsocketSession.COMMANDS_SUBSCRIPTION_LOCK, asyncMessageDeliverer));
            subscriptionManager.getCommandSubscriptionStorage().insert(commandSubscription);


            logger.debug("will get commands newer than : {}", timestamp);
            List<DeviceCommand> commandsFromDatabase = commandService.getNewerThan(device.getGuid(), timestamp);
            for (DeviceCommand deviceCommand : commandsFromDatabase) {
                logger.debug("will add command to queue : {}", deviceCommand.getId());
                WebsocketSession
                        .addMessagesToQueue(session, ServerResponsesFactory.createCommandInsertMessage(deviceCommand));
            }
        } finally {
            WebsocketSession.getCommandsSubscriptionsLock(session).unlock();
        }
        logger.debug("deliver messages for session {}", session.getId());
        asyncMessageDeliverer.deliverMessages(session);
        logger.debug("command subscribe ended for session : {}", session.getId());
        return JsonMessageBuilder.createSuccessResponseBuilder().build();
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#WsReference/Device/commandunsubscribe">WebSocket API:
     * Device: command/unsubscribe</a>
     * Unsubscribes the device from commands.
     *
     * @param message Json object with the following structure:
     *                <pre>
     *                               {
     *                                 "action": {string},
     *                                 "requestId": {object},
     *                                 "deviceId": {guid},
     *                                 "deviceKey": {string}
     *                               }
     *                               </pre>
     * @param session Current session
     * @return Json object with the following structure:
     *         <pre>
     *                 {
     *                   "action": {string},
     *                   "status": {string},
     *                   "requestId": {object}
     *                 }
     *                 </pre>
     */
    @Action(value = "command/unsubscribe")
    public JsonObject processNotificationUnsubscribe(JsonObject message, Session session) {
        Device device = getDevice(session, message);
        logger.debug("command/unsubscribe for device {}", device.getGuid());
        subscriptionManager.getCommandSubscriptionStorage().remove(device.getId(), session.getId());
        return JsonMessageBuilder.createSuccessResponseBuilder().build();
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#WsReference/Device/notificationinsert">WebSocket
     * API: Device: notification/insert</a>
     * Creates new device notification.
     *
     * @param message Json object with the following structure
     *                <pre>
     *                               {
     *                                 "action": {string},
     *                                 "requestId": {object},
     *                                 "deviceId": {guid},
     *                                 "deviceKey": {string},
     *                                 "notification": {
     *                                   "notification": {string},
     *                                   "parameters": {object}
     *                                 }
     *                               }
     *                               </pre>
     * @param session Current session
     * @return Json object with the following structure
     *         <pre>
     *                 {
     *                   "action": {string},
     *                   "status": {string},
     *                   "requestId": {object},
     *                   "notification": {
     *                     "id": {integer},
     *                     "timestamp": {datetime}
     *                   }
     *                 }
     *                 </pre>
     */
    @Action(value = "notification/insert")
    public JsonObject processNotificationInsert(JsonObject message, Session session) {
        logger.debug("notification/insert started for session {} ", session.getId());
        DeviceNotification deviceNotification = GsonFactory.createGson(NOTIFICATION_FROM_DEVICE)
                .fromJson(message.get("notification"), DeviceNotification.class);
        if (deviceNotification == null || deviceNotification.getNotification() == null) {
            throw new HiveException("Notification is empty!");
        }
        Device device = getDevice(session, message);
        logger.debug("process submit device notification started for deviceNotification : {}", deviceNotification
                .getNotification() + " and device : " + device.getGuid());
        deviceService.submitDeviceNotification(deviceNotification, device);
        JsonObject jsonObject = JsonMessageBuilder.createSuccessResponseBuilder().build();
        jsonObject.add("notification", GsonFactory.createGson(NOTIFICATION_TO_DEVICE).toJsonTree(deviceNotification));
        logger.debug("notification/insert ended for session {} ", session.getId());

        return jsonObject;
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#WsReference/Device/serverinfo">WebSocketAPI:
     * Device: server/info</a>
     * Gets meta-information of the current API.
     *
     * @param message Json object with the following structure
     *                <pre>
     *                               {
     *                                 "action": {string},
     *                                 "requestId": {object}
     *                               }
     *                               </pre>
     * @param session Current session
     * @return Json object with the following structure
     *         <pre>
     *                 {
     *                   "action": {string},
     *                   "status": {string},
     *                   "requestId": {object},
     *                   "info": {
     *                   "apiVersion": {string},
     *                     "serverTimestamp": {datetime},
     *                     "webSocketServerUrl": {string}
     *                 }
     *                 }
     *                 </pre>
     */
    @Action(value = "server/info", needsAuth = false)
    public JsonObject processServerInfo(JsonObject message, Session session) {
        logger.debug("server/info action started. Session {} ", session.getId());
        Gson gson = GsonFactory.createGson(WEBSOCKET_SERVER_INFO);
        ApiInfo apiInfo = new ApiInfo();
        apiInfo.setApiVersion(Constants.API_VERSION);
        apiInfo.setServerTimestamp(timestampService.getTimestamp());
        String url = configurationService.get(Constants.REST_SERVER_URL);
        if (url != null) {
            apiInfo.setRestServerUrl(url);
        }
        JsonObject jsonObject = JsonMessageBuilder.createSuccessResponseBuilder()
                .addElement("info", gson.toJsonTree(apiInfo))
                .build();
        logger.debug("server/info action completed. Session {} ", session.getId());
        return jsonObject;
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#WsReference/Device/deviceget">WebSocketAPI:
     * Device: device/get</a>
     * Gets information about the current device.
     *
     * @param message Json object with the following structure
     *                <pre>
     *                               {
     *                                 "action": {string},
     *                                 "requestId": {object},
     *                                 "deviceId": {guid},
     *                                 "deviceKey": {string}
     *                               }
     *                               </pre>
     * @param session Current session
     * @return Json object with the following structure
     *         <pre>
     *                 {
     *                   "action": {string},
     *                   "status": {string},
     *                   "requestId": {object},
     *                   "device": {
     *                     "id": {guid},
     *                     "name": {string},
     *                     "status": {string},
     *                     "data": {object},
     *                     "network": {
     *                       "id": {integer},
     *                       "name": {string},
     *                       "description": {string}
     *                     },
     *                     "deviceClass": {
     *                       "id": {integer},
     *                       "name": {string},
     *                       "version": {string},
     *                       "isPermanent": {boolean},
     *                       "offlineTimeout": {integer},
     *                       "data": {object}
     *                      }
     *                    }
     *                 }
     *                 </pre>
     */
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

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#WsReference/Device/devicesave">WebSocketAPI:
     * Device: device/save</a>
     * Registers or updates a device. A valid device key is required in the deviceKey parameter in order to update an
     * existing device.
     *
     * @param message Json object with the following structure
     *                <pre>
     *                               {
     *                                 "action": {string},
     *                                 "requestId": {object},
     *                                 "deviceId": {guid},
     *                                 "deviceKey": {string},
     *                                 "device": {
     *                                   "key": {string},
     *                                   "name": {string},
     *                                   "status": {string},
     *                                   "data": {object},
     *                                   "network": {integer or object},
     *                                   "deviceClass": {integer or object},
     *                                   "equipment": [
     *                                   {
     *                                    "name": {string},
     *                                    "code": {string},
     *                                    "type": {string},
     *                                    "data": {object}
     *                                   }
     *                                   ]
     *                                 }
     *                               }
     *                               </pre>
     * @param session Current session
     * @return Json object with the following structure
     *         <pre>
     *                 {
     *                   "action": {string},
     *                   "status": {string},
     *                   "requestId": {object}
     *                 }
     *                 </pre>
     */
    @Action(value = "device/save", needsAuth = false)
    public JsonObject processDeviceSave(JsonObject message, Session session) {
        logger.debug("device/save process started for session {}", session.getId());
        UUID deviceId = GsonFactory.createGson().fromJson(message.get("deviceId"), UUID.class);
        if (deviceId == null) {
            throw new HiveException("Device ID is undefined!");
        }
        if (message.get("deviceKey") == null) {
            throw new HiveException("Device key is undefined!");
        }
        Gson mainGson = GsonFactory.createGson(DEVICE_PUBLISHED);
        DeviceUpdate device = mainGson.fromJson(message.get("device"), DeviceUpdate.class);
        logger.debug("check required fields in device ");
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
        Device authorizedDevice = getDevice(session, message);
        boolean isAllowedToUpdate = authorizedDevice != null && authorizedDevice.getGuid().equals(device.getGuid()
                .getValue());
        deviceService.deviceSaveAndNotify(device, equipmentSet, useExistingEquipment, isAllowedToUpdate);
        JsonObject jsonResponseObject = JsonMessageBuilder.createSuccessResponseBuilder()
                .addAction("device/save")
                .addRequestId(message.get("requestId"))
                .build();
        logger.debug("device/save process ended for session  {}", session.getId());
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
