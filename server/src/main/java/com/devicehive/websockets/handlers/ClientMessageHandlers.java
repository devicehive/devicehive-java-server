package com.devicehive.websockets.handlers;

import com.devicehive.auth.AllowedAction;
import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.messages.handler.WebsocketHandlerCreator;
import com.devicehive.messages.subscriptions.NotificationSubscription;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.*;
import com.devicehive.service.*;
import com.devicehive.utils.LogExecutionTime;
import com.devicehive.utils.ServerResponsesFactory;
import com.devicehive.websockets.handlers.annotations.Action;
import com.devicehive.websockets.handlers.annotations.Authorize;
import com.devicehive.websockets.handlers.annotations.WsParam;
import com.devicehive.websockets.util.AsyncMessageSupplier;
import com.devicehive.utils.ThreadLocalVariablesKeeper;
import com.devicehive.websockets.util.WebSocketResponse;
import com.devicehive.websockets.util.WebsocketSession;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.websocket.Session;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;
import static com.devicehive.auth.AllowedAction.Action.*;

@LogExecutionTime
public class ClientMessageHandlers implements HiveMessageHandlers {

    private static final Logger logger = LoggerFactory.getLogger(ClientMessageHandlers.class);
    @EJB
    private SubscriptionManager subscriptionManager;
    @EJB
    private UserService userService;
    @EJB
    private DeviceService deviceService;
    @EJB
    private DeviceCommandService commandService;
    @EJB
    private DeviceDAO deviceDAO;
    @EJB
    private ConfigurationService configurationService;
    @EJB
    private DeviceNotificationService deviceNotificationService;
    @EJB
    private AsyncMessageSupplier asyncMessageDeliverer;
    @EJB
    private TimestampService timestampService;

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#WsReference/Client/authenticate">WebSocket API:
     * Client: authenticate</a>
     * Authenticates a user.
     *
     * @param session Current session
     * @return JsonObject with structure
     *         <pre>
     *                                         {
     *                                           "action": {string},
     *                                           "status": {string},
     *                                           "requestId": {object}
     *                                         }
     *                                         </pre>
     *         Where:
     *         action - Action name: authenticate
     *         status - Operation execution status (success or error).
     *         requestId - Request unique identifier as specified in the request message.
     */
    @Action(value = "authenticate")
    @PermitAll
    @Authorize
    public WebSocketResponse processAuthenticate(@WsParam("login")String login, @WsParam("password")String password,
                                          Session session) {
        if (login == null || password == null) {
            throw new HiveException("login and password cannot be empty!");
        }
        logger.debug("authenticate action for {} ", login);
        User user = ThreadLocalVariablesKeeper.getPrincipal().getUser();

        if (user != null) {
            WebsocketSession.setAuthorisedUser(session, user);
            return new WebSocketResponse();
        } else {
            throw new HiveException("Client authentication error: credentials are incorrect");
        }
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#WsReference/Client/commandinsert">WebSocket API:
     * Client: command/insert</a>
     * Creates new device command.
     *
     * @param session Current session
     * @return JsonObject with structure:
     *         <pre></pre>
     *         {
     *         "action": {string},
     *         "status": {string},
     *         "requestId": {object},
     *         "command": {
     *         "id": {integer},
     *         "timestamp": {datetime},
     *         "userId": {integer}
     *         }
     *         }
     *         </pre>
     */
    @Action(value = "command/insert")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedAction(action = {CREATE_DEVICE_COMMAND})
    @Authorize
    public WebSocketResponse processCommandInsert(@WsParam(JsonMessageBuilder.DEVICE_GUID) String deviceGuid,
                                           @WsParam("command") @JsonPolicyDef(COMMAND_FROM_CLIENT) DeviceCommand deviceCommand,
                                           Session session) {
        logger.debug("command/insert action for {}, Session ", deviceGuid, session.getId());
        if (deviceGuid == null) {
            throw new HiveException("Device ID is empty");
        }

        User user = WebsocketSession.getAuthorisedUser(session);
        Device device;
        if (user.getRole() == UserRole.ADMIN) {
            device = deviceDAO.findByUUID(deviceGuid);
        } else {
            device = deviceDAO.findByUUIDAndUser(user, deviceGuid);
        }

        if (device == null) {
            throw new HiveException("Unknown Device ID");
        }
        if (deviceCommand == null) {
            throw new HiveException("Command is empty");
        }
        deviceCommand.setUserId(user.getId());

        commandService.submitDeviceCommand(deviceCommand, device, user, session);
        WebSocketResponse response =  new WebSocketResponse();
        response.addValue("command", deviceCommand, COMMAND_TO_CLIENT);
        return response;
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#WsReference/Client/notificationsubscribe">
     * WebSocket API: Client: notification/subscribe</a>
     * Subscribes to device notifications. After subscription is completed,
     * the server will start to send notification/insert messages to the connected user.
     *
     * @param session Current session
     * @return Json object with the following structure:
     *         <pre>
     *                                         {
     *                                           "action": {string},
     *                                           "status": {string},
     *                                           "requestId": {object}
     *                                         }
     *                                         </pre>
     * @throws IOException if unable to deliver message
     */
    @Action(value = "notification/subscribe")
    @RolesAllowed({HiveRoles.ADMIN, HiveRoles.CLIENT, HiveRoles.KEY})
    @AllowedAction(action = {GET_DEVICE_NOTIFICATION})
    @Authorize
    public WebSocketResponse processNotificationSubscribe(@WsParam(JsonMessageBuilder.DEVICE_GUIDS) List<String> list,
                                                   @WsParam(JsonMessageBuilder.TIMESTAMP) Timestamp timestamp,
                                                   Session session) throws IOException {
        logger.debug("notification/subscribe action. Session {} ", session.getId());
        if (timestamp == null) {
            timestamp = timestampService.getTimestamp();
        }

        if (list == null || list.isEmpty()) {
            prepareForNotificationSubscribeNullCase(session, timestamp);
        } else {
            prepareForNotificationSubscribeNotNullCase(list, session, timestamp);
        }
        logger.debug("notification/subscribe action  finished");
        return new WebSocketResponse();

    }

    private void prepareForNotificationSubscribeNullCase(Session session, Timestamp timestamp) throws IOException {
        logger.debug("notification/subscribe action - null guid case. Session {}", session.getId());
        User authorizedUser = WebsocketSession.getAuthorisedUser(session);
        List<DeviceNotification> deviceNotifications;
        if (authorizedUser.getRole() == UserRole.ADMIN) {
            deviceNotifications =
                    deviceNotificationService.getDeviceNotificationList(null, authorizedUser, timestamp, true);
        } else {
            deviceNotifications =
                    deviceNotificationService.getDeviceNotificationList(null, authorizedUser, timestamp, false);
        }
        logger.debug(
                "notification/subscribe action - null guid case. get device notification. found {}  notifications. {}",
                deviceNotifications.size(), session.getId());
        notificationSubscribeAction(deviceNotifications, session, null);
    }

    private void prepareForNotificationSubscribeNotNullCase(List<String> guids, Session session, Timestamp timestamp)
            throws IOException {
        logger.debug("notification/subscribe action - null guid case. Session {}", session.getId());
        User authorizedUser = WebsocketSession.getAuthorisedUser(session);
        List<Device> devices;
        if (authorizedUser.getRole() == UserRole.ADMIN) {
            devices = deviceDAO.findByUUID(guids);
        } else {
            devices = deviceDAO.findByUUIDListAndUser(authorizedUser, guids);
        }
        if (devices.isEmpty()) {
            logger.debug("No devices found. Return " + ". Session {} ", session.getId());
            throw new HiveException("No available devices found.");
        }

        logger.debug("Found " + devices.size() + " devices" + ". Session " + session.getId());
        List<DeviceNotification> deviceNotifications =
                deviceNotificationService.getDeviceNotificationList(devices, authorizedUser, timestamp, null);

        notificationSubscribeAction(deviceNotifications, session, devices);

        checkDevicesAndGuidsList(devices, guids, true);
    }

    private void notificationSubscribeAction(List<DeviceNotification> deviceNotifications, Session session,
                                             List<Device> devices)
            throws IOException {
        try {
            logger.debug("notification/subscribe action - not null guid case. found {} devices. Session {}",
                    deviceNotifications.size(), session.getId());
            WebsocketSession.getNotificationSubscriptionsLock(session).lock();

            User user = WebsocketSession.getAuthorisedUser(session);
            if (devices != null) {
                List<NotificationSubscription> nsList = new ArrayList<>();
                for (Device device : devices) {
                    NotificationSubscription ns = new NotificationSubscription(user, device.getId(), session.getId(),
                            new WebsocketHandlerCreator(session, WebsocketSession.NOTIFICATIONS_LOCK,
                                    asyncMessageDeliverer));
                    nsList.add(ns);
                }
                subscriptionManager.getNotificationSubscriptionStorage().insertAll(nsList);
            } else {
                NotificationSubscription forAll =
                        new NotificationSubscription(user, Constants.DEVICE_NOTIFICATION_NULL_ID_SUBSTITUTE,
                                session.getId(),
                                new WebsocketHandlerCreator(session, WebsocketSession.NOTIFICATIONS_LOCK,
                                        asyncMessageDeliverer));
                subscriptionManager.getNotificationSubscriptionStorage().insert(forAll);
            }
            if (!deviceNotifications.isEmpty()) {
                for (DeviceNotification deviceNotification : deviceNotifications) {
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

    /**
     * Implementation of the <a href="http://www.devicehive.com/restful#WsReference/Client/notificationunsubscribe">
     * WebSocket API: Client: notification/unsubscribe</a>
     * Unsubscribes from device notifications.
     * @param list devices' guids list
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
    @Action(value = "notification/unsubscribe")
    @RolesAllowed({HiveRoles.ADMIN, HiveRoles.CLIENT, HiveRoles.KEY})
    @AllowedAction(action = {GET_DEVICE_NOTIFICATION})
    @Authorize
    public WebSocketResponse processNotificationUnsubscribe(@WsParam(JsonMessageBuilder.DEVICE_GUIDS) List<String> list,
                                                     Session session) {
        logger.debug("notification/unsubscribe action. Session {} ", session.getId());
        try {
            WebsocketSession.getNotificationSubscriptionsLock(session).lock();
            List<Device> devices = null;
            List<Pair<Long, String>> subs;
            if (list != null && !list.isEmpty()) {
                devices = deviceDAO.findByUUID(list);
                logger.debug("notification/unsubscribe. found {} devices. ", devices.size());
                if (devices.isEmpty()) {
                    throw new HiveException("No available devices found");
                }

                logger.debug("notification/unsubscribe. performing unsubscribing action");

                subs = new ArrayList<>(devices.size());
                for (Device device : devices) {
                    subs.add(ImmutablePair.of(device.getId(), session.getId()));
                }
            } else {
                subs = new ArrayList<>(1);
                subs.add(ImmutablePair.of(Constants.DEVICE_NOTIFICATION_NULL_ID_SUBSTITUTE, session.getId()));

            }
            subscriptionManager.getNotificationSubscriptionStorage().removePairs(subs);

            checkDevicesAndGuidsList(devices, list, false);
        } finally {
            WebsocketSession.getNotificationSubscriptionsLock(session).unlock();
        }

        logger.debug("notification/unsubscribe completed for session {}", session.getId());
        return new WebSocketResponse();
    }

    private void checkDevicesAndGuidsList(List<Device> devices, List<String> guids, boolean isSubscribe) {
        if (devices == null && (guids == null || guids.size() == 0)) {
            return;
        }
        if (devices == null || devices.size() != guids.size()) {
            StringBuilder responseBuilder;
            if (isSubscribe) {
                responseBuilder = new StringBuilder("Unable to subscribe for devices with guids: ");
            } else {
                responseBuilder = new StringBuilder("Unable to unsubscribe from devices with guids: ");
            }

            for (String guid : guids) {
                boolean contains = false;
                if (devices != null) {
                    for (Device device : devices) {
                        if (device.getGuid().equals(guid)) {
                            contains = true;
                        }
                    }
                }
                if (!contains) {
                    responseBuilder.append(guid).append(" ");
                }
            }

            responseBuilder
                    .append(". Device(s) with such guids does not exist(s) or you have not permissions to get " +
                            "notifications from this device.");
            throw new HiveException(responseBuilder.toString());
        }

    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#WsReference/Client/serverinfo">WebSocket API:
     * Client: server/info</a>
     * Gets meta-information about the current API.
     *
     * @param session Current session
     * @return Json object with the following structure
     *         <pre>
     *                                 {
     *                                   "action": {string},
     *                                   "status": {string},
     *                                   "requestId": {object},
     *                                   "info": {
     *                                     "apiVersion": {string},
     *                                     "serverTimestamp": {datetime},
     *                                     "restServerUrl": {string}
     *                                   }
     *                                 }
     *                                 </pre>
     */

    @Action(value = "server/info")
    @PermitAll
    @Authorize
    public WebSocketResponse processServerInfo(Session session) {
        logger.debug("server/info action started. Session " + session.getId());
        ApiInfo apiInfo = new ApiInfo();
        apiInfo.setApiVersion(Constants.API_VERSION);
        apiInfo.setServerTimestamp(timestampService.getTimestamp());
        String url = configurationService.get(Constants.REST_SERVER_URL);
        if (url != null) {
            apiInfo.setRestServerUrl(url);
        }
        WebSocketResponse response =  new WebSocketResponse();
        response.addValue("info",apiInfo, WEBSOCKET_SERVER_INFO);
        logger.debug("server/info action completed. Session {}", session.getId());
        return response;
    }


}