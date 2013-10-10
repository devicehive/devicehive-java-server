package com.devicehive.websockets.handlers;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
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
import com.devicehive.utils.ThreadLocalVariablesKeeper;
import com.devicehive.websockets.handlers.annotations.Action;
import com.devicehive.websockets.handlers.annotations.Authorize;
import com.devicehive.websockets.handlers.annotations.WsParam;
import com.devicehive.websockets.util.AsyncMessageSupplier;
import com.devicehive.websockets.util.WebSocketResponse;
import com.devicehive.websockets.util.WebsocketSession;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.websocket.Session;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

@LogExecutionTime
@Authorize
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
     *                                                 {
     *                                                   "action": {string},
     *                                                   "status": {string},
     *                                                   "requestId": {object}
     *                                                 }
     *                                                 </pre>
     */
    @Action(value = "authenticate")
    @PermitAll
    public WebSocketResponse processAuthenticate(@WsParam("deviceId") String deviceId,
                                                 @WsParam("deviceKey") String deviceKey,
                                                 Session session) {

        if (deviceId == null || deviceKey == null) {
            throw new HiveException("Device authentication error: credentials are incorrect", SC_UNAUTHORIZED);
        }
        logger.debug("authenticate action for " + deviceId);
        Device device = deviceService.authenticate(deviceId, deviceKey);

        if (device != null) {
            HivePrincipal hivePrincipal = WebsocketSession.getPrincipal(session);
            if (hivePrincipal == null) {
                hivePrincipal = new HivePrincipal(null, device, null);
            } else {
                hivePrincipal = new HivePrincipal(hivePrincipal.getUser(), device, hivePrincipal.getKey());
            }
            WebsocketSession.setPrincipal(session, hivePrincipal);
            return new WebSocketResponse();
        } else {
            throw new HiveException("Device authentication error: credentials are incorrect", SC_UNAUTHORIZED);
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
     *                                                 {
     *                                                   "action": {string},
     *                                                   "status": {string},
     *                                                   "requestId": {object}
     *                                                 }
     *                                                 </pre>
     */
    @Action(value = "command/update")
    @RolesAllowed({HiveRoles.DEVICE})
    public WebSocketResponse processCommandUpdate(@WsParam("commandId") Long commandId,
                                                  @WsParam("command") @JsonPolicyDef(COMMAND_UPDATE_FROM_DEVICE)
                                                  DeviceCommandUpdate update,
                                                  Session session) {
        logger.debug("command update action started for session : {{}", session.getId());
        if (commandId == null) {
            throw new HiveException("Device command identifier cannot be null", SC_BAD_REQUEST);
        }
        if (update == null) {
            throw new HiveException("DeviceCommand resource cannot be null", SC_BAD_REQUEST);
        }
        update.setId(commandId);
        Device device = ThreadLocalVariablesKeeper.getPrincipal().getDevice();

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
     *                                                 {
     *                                                   "action": {string},
     *                                                   "status": {string},
     *                                                   "requestId": {object}
     *                                                 }
     *                                                 </pre>
     * @throws IOException if unable to deliver message
     */
    @Action(value = "command/subscribe")
    @RolesAllowed({HiveRoles.DEVICE})
    public WebSocketResponse processCommandSubscribe(@WsParam("deviceId") String deviceId,
                                                     @WsParam(JsonMessageBuilder.TIMESTAMP) Timestamp timestamp,
                                                     Session session) throws IOException {
        logger.debug("command subscribe action started for session : " + session.getId());
        Device device = ThreadLocalVariablesKeeper.getPrincipal().getDevice();
        if (timestamp == null) {
            timestamp = timestampService.getTimestamp();
        }
        try {
            WebsocketSession.getCommandsSubscriptionsLock(session).lock();
            logger.debug("will subscribe device for commands : " + device.getGuid());

            CommandSubscription commandSubscription = new CommandSubscription(
                    ThreadLocalVariablesKeeper.getPrincipal(),
                    device.getId(),
                    session.getId(),
                    new WebsocketHandlerCreator(session, WebsocketSession.COMMANDS_SUBSCRIPTION_LOCK,
                            asyncMessageDeliverer));
            subscriptionManager.getCommandSubscriptionStorage().insert(commandSubscription);


            logger.debug("will get commands newer than : {}", timestamp);
            List<DeviceCommand> commandsFromDatabase = commandService.getNewerThan(Arrays.asList(device), null, timestamp);
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
     *                                                 {
     *                                                   "action": {string},
     *                                                   "status": {string},
     *                                                   "requestId": {object}
     *                                                 }
     *                                                 </pre>
     */
    @Action(value = "command/unsubscribe")
    @RolesAllowed({HiveRoles.DEVICE})
    public WebSocketResponse processCommandUnsubscribe(Session session) {
        Device device = ThreadLocalVariablesKeeper.getPrincipal().getDevice();
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
     *                                                 {
     *                                                   "action": {string},
     *                                                   "status": {string},
     *                                                   "requestId": {object},
     *                                                   "notification": {
     *                                                     "id": {integer},
     *                                                     "timestamp": {datetime}
     *                                                   }
     *                                                 }
     *                                                 </pre>
     */
    @Action(value = "notification/insert")
    @RolesAllowed({HiveRoles.DEVICE})
    public WebSocketResponse processNotificationInsert(@WsParam("notification") @JsonPolicyDef(NOTIFICATION_FROM_DEVICE)
                                                       DeviceNotification deviceNotification, Session session) {
        logger.debug("notification/insert started for session {} ", session.getId());

        if (deviceNotification == null || deviceNotification.getNotification() == null) {
            throw new HiveException("Notification is empty!", SC_BAD_REQUEST);
        }
        Device device = ThreadLocalVariablesKeeper.getPrincipal().getDevice();
        logger.debug("process submit device notification started for deviceNotification : {}", deviceNotification
                .getNotification() + " and device : " + device.getGuid());
        deviceNotificationService.submitDeviceNotification(deviceNotification, device);
        WebSocketResponse response = new WebSocketResponse();
        response.addValue("notification", deviceNotification, NOTIFICATION_TO_DEVICE);
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
     *                                                 {
     *                                                   "action": {string},
     *                                                   "status": {string},
     *                                                   "requestId": {object},
     *                                                   "info": {
     *                                                   "apiVersion": {string},
     *                                                     "serverTimestamp": {datetime},
     *                                                     "webSocketServerUrl": {string}
     *                                                 }
     *                                                 }
     *                                                 </pre>
     */
    @Action(value = "server/info")
    @PermitAll
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
     * @return Json object with the following structure
     *         <pre>
     *                                                 {
     *                                                   "action": {string},
     *                                                   "status": {string},
     *                                                   "requestId": {object},
     *                                                   "device": {
     *                                                     "id": {guid},
     *                                                     "name": {string},
     *                                                     "status": {string},
     *                                                     "data": {object},
     *                                                     "network": {
     *                                                       "id": {integer},
     *                                                       "name": {string},
     *                                                       "description": {string}
     *                                                     },
     *                                                     "deviceClass": {
     *                                                       "id": {integer},
     *                                                       "name": {string},
     *                                                       "version": {string},
     *                                                       "isPermanent": {boolean},
     *                                                       "offlineTimeout": {integer},
     *                                                       "data": {object}
     *                                                      }
     *                                                    }
     *                                                 }
     *                                                 </pre>
     */
    @Action(value = "device/get")
    @RolesAllowed({HiveRoles.DEVICE})
    public WebSocketResponse processDeviceGet() {
        Device device = ThreadLocalVariablesKeeper.getPrincipal().getDevice();
        Device toResponse = device == null ? null : deviceDAO.findByUUIDWithNetworkAndDeviceClass(device.getGuid());
        WebSocketResponse response = new WebSocketResponse();
        response.addValue("device", toResponse, DEVICE_PUBLISHED_DEVICE_AUTH);
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
     *                                                                                           {
     *                                                                                             "action": {string},
     *                                                                                             "requestId": {object},
     *                                                                                             "deviceId": {guid},
     *                                                                                             "deviceKey": {string},
     *                                                                                             "device": {
     *                                                                                               "key": {string},
     *                                                                                               "name": {string},
     *                                                                                               "status": {string},
     *                                                                                               "data": {object},
     *                                                                                               "network": {integer or object},
     *                                                                                               "deviceClass": {integer or object},
     *                                                                                               "equipment": [
     *                                                                                               {
     *                                                                                                "name": {string},
     *                                                                                                "code": {string},
     *                                                                                                "type": {string},
     *                                                                                                "data": {object}
     *                                                                                               }
     *                                                                                               ]
     *                                                                                             }
     *                                                                                           }
     *                                                                                           </pre>
     * @param session Current session
     * @return Json object with the following structure
     *         <pre>
     *                                                 {
     *                                                   "action": {string},
     *                                                   "status": {string},
     *                                                   "requestId": {object}
     *                                                 }
     *                                                 </pre>
     */
    @Action(value = "device/save")
    @PermitAll
    public WebSocketResponse processDeviceSave(@WsParam("deviceId") String deviceId,
                                               @WsParam("deviceKey") String deviceKey,
                                               @WsParam("device") @JsonPolicyDef(DEVICE_PUBLISHED) DeviceUpdate device,
                                               JsonObject message,
                                               Session session) {
        logger.debug("device/save process started for session {}", session.getId());
        if (deviceId == null) {
            throw new HiveException("Device ID is undefined!", SC_BAD_REQUEST);
        }
        if (deviceKey == null) {
            throw new HiveException("Device key is undefined!", SC_BAD_REQUEST);
        }
        device.setGuid(new NullableWrapper<>(deviceId));
        Gson gsonForEquipment = GsonFactory.createGson();
        boolean useExistingEquipment = message.get("equipment") == null;
        Set<Equipment> equipmentSet = gsonForEquipment.fromJson(
                message.get("equipment"),
                new TypeToken<HashSet<Equipment>>() {
                }.getType());

        if (equipmentSet != null) {
            equipmentSet.remove(null);
        }
        deviceService.deviceSaveAndNotify(device, equipmentSet, ThreadLocalVariablesKeeper.getPrincipal(),
                useExistingEquipment);
        logger.debug("device/save process ended for session  {}", session.getId());
        return new WebSocketResponse();
    }

}
