package com.devicehive.websockets.handlers;

import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.GsonFactory;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.messages.handler.WebsocketHandlerCreator;
import com.devicehive.messages.subscriptions.CommandSubscription;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.*;
import com.devicehive.model.updates.DeviceCommandUpdate;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.service.*;
import com.devicehive.utils.LogExecutionTime;
import com.devicehive.utils.ServerResponsesFactory;
import com.devicehive.websockets.handlers.annotations.Action;
import com.devicehive.websockets.handlers.annotations.WsParam;
import com.devicehive.websockets.util.AsyncMessageSupplier;
import com.devicehive.websockets.util.WebSocketResponse;
import com.devicehive.websockets.util.WebsocketSession;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
    private DeviceNotificationService deviceNotificationService;
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
     * @param session Current session.
     * @return Json object with the following structure
     *         <pre>
     *                                 {
     *                                   "action": {string},
     *                                   "status": {string},
     *                                   "requestId": {object}
     *                                 }
     *                                 </pre>
     */
    @Action(value = "authenticate", needsAuth = false)
    public WebSocketResponse processAuthenticate(@WsParam("deviceId") String deviceId,
                                          @WsParam("deviceKey") String deviceKey,
                                          Session session) {

        if (deviceId == null || deviceKey == null) {
            throw new HiveException("Device authentication error: credentials are incorrect");
        }
        logger.debug("authenticate action for " + deviceId);
        Device device = deviceService.authenticate(deviceId, deviceKey);

        if (device != null) {
            WebsocketSession.setAuthorisedDevice(session, device);
            return new WebSocketResponse();
        } else {
            throw new HiveException("Device authentication error: credentials are incorrect");
        }
    }

    @Override
    public void ensureAuthorised(JsonObject request, Session session) {
        if (WebsocketSession.hasAuthorisedDevice(session)) {
            return;
        }
        String deviceId = request.get("deviceId").getAsString();
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
     * @param session Current session.
     * @return Json object with the following structure:
     *         <pre>
     *                                 {
     *                                   "action": {string},
     *                                   "status": {string},
     *                                   "requestId": {object}
     *                                 }
     *                                 </pre>
     */
    @Action(value = "command/update")
    public WebSocketResponse processCommandUpdate(@WsParam("commandId") Long commandId,
                                           @WsParam("command") @JsonPolicyDef(COMMAND_UPDATE_FROM_DEVICE)DeviceCommandUpdate update,
                                           @WsParam("deviceId") String deviceId,
                                           Session session) {
        logger.debug("command update action started for session : {{}", session.getId());
        if (commandId == null) {
            throw new HiveException("Device command identifier cannot be null");
        }
        if (update == null) {
            throw new HiveException("DeviceCommand resource cannot be null");
        }
        update.setId(commandId);
        Device device = getDevice(session, deviceId);

        logger.debug("submit device command update for device : " + device.getId());
        commandService.submitDeviceCommandUpdate(update, device);

        logger.debug("command update action finished for session : " + session.getId());
        return new WebSocketResponse();
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#WsReference/Device/commandsubscribe">WebSocket API:
     * Device: command/subscribe</a>
     * Subscribes the device to commands. After subscription is completed, the server will start to send
     * command/insert messages to the connected device.
     *
     * @param session Current session
     * @return json object with the following structure:
     *         <pre>
     *                                 {
     *                                   "action": {string},
     *                                   "status": {string},
     *                                   "requestId": {object}
     *                                 }
     *                                 </pre>
     * @throws IOException if unable to deliver message
     */
    @Action(value = "command/subscribe")
    public WebSocketResponse processCommandSubscribe(@WsParam("deviceId") String deviceId,
                                              @WsParam(JsonMessageBuilder.TIMESTAMP) Timestamp timestamp,
                                              Session session) throws IOException {
        logger.debug("command subscribe action started for session : " + session.getId());
        Device device = getDevice(session, deviceId);
        if (timestamp == null) {
            timestamp = timestampService.getTimestamp();
        }
        try {
            WebsocketSession.getCommandsSubscriptionsLock(session).lock();
            logger.debug("will subscribe device for commands : " + device.getGuid());

            CommandSubscription commandSubscription = new CommandSubscription(device.getId(), session.getId(),
                    new WebsocketHandlerCreator(session, WebsocketSession.COMMANDS_SUBSCRIPTION_LOCK,
                            asyncMessageDeliverer));
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
        return new WebSocketResponse();
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#WsReference/Device/commandunsubscribe">WebSocket API:
     * Device: command/unsubscribe</a>
     * Unsubscribes the device from commands.
     *
     * @param session Current session
     * @return Json object with the following structure:
     *         <pre>
     *                                 {
     *                                   "action": {string},
     *                                   "status": {string},
     *                                   "requestId": {object}
     *                                 }
     *                                 </pre>
     */
    @Action(value = "command/unsubscribe")
    public WebSocketResponse processNotificationUnsubscribe(@WsParam("deviceId") String deviceId, Session session) {
        Device device = getDevice(session, deviceId);
        logger.debug("command/unsubscribe for device {}", device.getGuid());
        subscriptionManager.getCommandSubscriptionStorage().remove(device.getId(), session.getId());
        return new WebSocketResponse();
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#WsReference/Device/notificationinsert">WebSocket
     * API: Device: notification/insert</a>
     * Creates new device notification.
     *
     * @param session Current session
     * @return Json object with the following structure
     *         <pre>
     *                                 {
     *                                   "action": {string},
     *                                   "status": {string},
     *                                   "requestId": {object},
     *                                   "notification": {
     *                                     "id": {integer},
     *                                     "timestamp": {datetime}
     *                                   }
     *                                 }
     *                                 </pre>
     */
    @Action(value = "notification/insert")
    public WebSocketResponse processNotificationInsert(@WsParam("notification") @JsonPolicyDef(NOTIFICATION_FROM_DEVICE)
                                           DeviceNotification deviceNotification,
                                                @WsParam("deviceId") String deviceId,
                                                Session session) {
        logger.debug("notification/insert started for session {} ", session.getId());

        if (deviceNotification == null || deviceNotification.getNotification() == null) {
            throw new HiveException("Notification is empty!");
        }
        Device device = getDevice(session, deviceId);
        logger.debug("process submit device notification started for deviceNotification : {}", deviceNotification
                .getNotification() + " and device : " + device.getGuid());
        deviceNotificationService.submitDeviceNotification(deviceNotification, device);
        WebSocketResponse response = new WebSocketResponse();
        response.addValue("notification", deviceNotification,NOTIFICATION_TO_DEVICE);
        logger.debug("notification/insert ended for session {} ", session.getId());
        return response;
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#WsReference/Device/serverinfo">WebSocketAPI:
     * Device: server/info</a>
     * Gets meta-information of the current API.
     *
     * @param session Current session
     * @return Json object with the following structure
     *         <pre>
     *                                 {
     *                                   "action": {string},
     *                                   "status": {string},
     *                                   "requestId": {object},
     *                                   "info": {
     *                                   "apiVersion": {string},
     *                                     "serverTimestamp": {datetime},
     *                                     "webSocketServerUrl": {string}
     *                                 }
     *                                 }
     *                                 </pre>
     */
    @Action(value = "server/info", needsAuth = false)
    public WebSocketResponse processServerInfo(Session session) {
        logger.debug("server/info action started. Session {} ", session.getId());
        ApiInfo apiInfo = new ApiInfo();
        apiInfo.setApiVersion(Constants.API_VERSION);
        apiInfo.setServerTimestamp(timestampService.getTimestamp());
        String url = configurationService.get(Constants.REST_SERVER_URL);
        if (url != null) {
            apiInfo.setRestServerUrl(url);
        }
        WebSocketResponse response = new WebSocketResponse();
        response.addValue("info", apiInfo, WEBSOCKET_SERVER_INFO);
        logger.debug("server/info action completed. Session {} ", session.getId());
        return response;
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#WsReference/Device/deviceget">WebSocketAPI:
     * Device: device/get</a>
     * Gets information about the current device.
     *
     * @param session Current session
     * @return Json object with the following structure
     *         <pre>
     *                                 {
     *                                   "action": {string},
     *                                   "status": {string},
     *                                   "requestId": {object},
     *                                   "device": {
     *                                     "id": {guid},
     *                                     "name": {string},
     *                                     "status": {string},
     *                                     "data": {object},
     *                                     "network": {
     *                                       "id": {integer},
     *                                       "name": {string},
     *                                       "description": {string}
     *                                     },
     *                                     "deviceClass": {
     *                                       "id": {integer},
     *                                       "name": {string},
     *                                       "version": {string},
     *                                       "isPermanent": {boolean},
     *                                       "offlineTimeout": {integer},
     *                                       "data": {object}
     *                                      }
     *                                    }
     *                                 }
     *                                 </pre>
     */
    @Action(value = "device/get")
    public WebSocketResponse processDeviceGet(@WsParam("deviceId") String deviceId, Session session) {
        Device device = getDevice(session, deviceId);
        Device toResponse = device == null ? null : deviceDAO.findByUUIDWithNetworkAndDeviceClass(device.getGuid());
        WebSocketResponse response = new WebSocketResponse();
        response.addValue("device", toResponse, DEVICE_PUBLISHED);
        return response;
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#WsReference/Device/devicesave">WebSocketAPI:
     * Device: device/save</a>
     * Registers or updates a device. A valid device key is required in the deviceKey parameter in order to update an
     * existing device.
     *
     * @param message Json object with the following structure
     *                <pre>
     *                                                             {
     *                                                               "action": {string},
     *                                                               "requestId": {object},
     *                                                               "deviceId": {guid},
     *                                                               "deviceKey": {string},
     *                                                               "device": {
     *                                                                 "key": {string},
     *                                                                 "name": {string},
     *                                                                 "status": {string},
     *                                                                 "data": {object},
     *                                                                 "network": {integer or object},
     *                                                                 "deviceClass": {integer or object},
     *                                                                 "equipment": [
     *                                                                 {
     *                                                                  "name": {string},
     *                                                                  "code": {string},
     *                                                                  "type": {string},
     *                                                                  "data": {object}
     *                                                                 }
     *                                                                 ]
     *                                                               }
     *                                                             }
     *                                                             </pre>
     * @param session Current session
     * @return Json object with the following structure
     *         <pre>
     *                                 {
     *                                   "action": {string},
     *                                   "status": {string},
     *                                   "requestId": {object}
     *                                 }
     *                                 </pre>
     */
    @Action(value = "device/save", needsAuth = false)
    public WebSocketResponse processDeviceSave(@WsParam("deviceId") String deviceId,
                                        @WsParam("deviceKey") String deviceKey,
                                        @WsParam("device") @JsonPolicyDef(DEVICE_PUBLISHED) DeviceUpdate device,
                                        JsonObject message,
                                        Session session) {
        logger.debug("device/save process started for session {}", session.getId());
        if (deviceId == null) {
            throw new HiveException("Device ID is undefined!");
        }
        if (deviceKey == null) {
            throw new HiveException("Device key is undefined!");
        }
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

        NullableWrapper<String> uuidNullableWrapper = new NullableWrapper<>();
        uuidNullableWrapper.setValue(deviceId);

        device.setGuid(uuidNullableWrapper);
        Device authorizedDevice = getDevice(session, deviceId);
        boolean isAllowedToUpdate = authorizedDevice != null && authorizedDevice.getGuid().equals(device.getGuid()
                .getValue());
        deviceService.deviceSaveAndNotify(device, equipmentSet, useExistingEquipment, isAllowedToUpdate);
        logger.debug("device/save process ended for session  {}", session.getId());
        return new WebSocketResponse();
    }

    private Device getDevice(Session session, String deviceId) {
        if (WebsocketSession.hasAuthorisedDevice(session)) {
            return WebsocketSession.getAuthorisedDevice(session);
        }
        return deviceDAO.findByUUID(deviceId);
    }


}
