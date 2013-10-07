package com.devicehive.websockets.handlers;

import com.devicehive.auth.AllowedKeyAction;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.messages.handler.WebsocketHandlerCreator;
import com.devicehive.messages.subscriptions.CommandSubscription;
import com.devicehive.messages.subscriptions.NotificationSubscription;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.*;
import com.devicehive.model.updates.DeviceCommandUpdate;
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
import java.util.*;

import static com.devicehive.auth.AllowedKeyAction.Action.*;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;
import static javax.servlet.http.HttpServletResponse.*;

@LogExecutionTime
@Authorize
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
    @EJB
    private AccessKeyService accessKeyService;

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#WsReference/Client/authenticate">WebSocket API:
     * Client: authenticate</a>
     * Authenticates a user.
     *
     * @param session Current session
     * @return JsonObject with structure
     *         <pre>
     *                                                                                                                                                                                         {
     *                                                                                                                                                                                           "action": {string},
     *                                                                                                                                                                                           "status": {string},
     *                                                                                                                                                                                           "requestId": {object}
     *                                                                                                                                                                                         }
     *                                                                                                                                                                                         </pre>
     *         Where:
     *         action - Action name: authenticate
     *         status - Operation execution status (success or error).
     *         requestId - Request unique identifier as specified in the request message.
     */
    @Action(value = "authenticate")
    @PermitAll
    public WebSocketResponse processAuthenticate(@WsParam("login") String login,
                                                 @WsParam("password") String password,
                                                 @WsParam("accessKey") String key,
                                                 Session session) {
        if ((login == null || password == null) && key == null) {
            throw new HiveException("login and password and key cannot be empty!", SC_BAD_REQUEST);
        }
        logger.debug("authenticate action for {} ", login);
        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        User user = principal.getUser() == null ? principal.getKey().getUser() : principal.getUser();
        if (user != null) {
            WebsocketSession.setAuthorisedUser(session, user);
            return new WebSocketResponse();
        } else {
            throw new HiveException("Client authentication error: credentials are incorrect", SC_UNAUTHORIZED);
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
    @AllowedKeyAction(action = {CREATE_DEVICE_COMMAND})
    public WebSocketResponse processCommandInsert(@WsParam(JsonMessageBuilder.DEVICE_GUID) String deviceGuid,
                                                  @WsParam("command") @JsonPolicyDef(COMMAND_FROM_CLIENT)
                                                  DeviceCommand deviceCommand,
                                                  Session session) {
        logger.debug("command/insert action for {}, Session ", deviceGuid, session.getId());
        if (deviceGuid == null) {
            throw new HiveException("Device ID is empty", SC_BAD_REQUEST);
        }
        User authUser = WebsocketSession.getAuthorisedUser(session);
        AccessKey authKey = WebsocketSession.getAuthorizedAccessKey(session);
        User user = WebsocketSession.hasAuthorisedUser(session) ? authUser : authKey.getUser();
        if (!WebsocketSession.hasAuthorisedUser(session) && !accessKeyService.hasAccessToDevice(WebsocketSession
                .getAuthorizedAccessKey(session), deviceGuid)) {
            logger.debug("No permissions to access device with guid {} for access key with id {}", deviceGuid,
                    WebsocketSession.getAuthorizedAccessKey(session).getId());
            throw new HiveException("Unknown device ID", SC_BAD_REQUEST);
        }
        Device device = deviceService.getDevice(deviceGuid, user, null);
        if (deviceCommand == null) {
            throw new HiveException("Command is empty", SC_BAD_REQUEST);
        }
        deviceCommand.setUserId(user.getId());

        commandService.submitDeviceCommand(deviceCommand, device, user, session);
        WebSocketResponse response = new WebSocketResponse();
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
     *                                                                                                                                                                                         {
     *                                                                                                                                                                                           "action": {string},
     *                                                                                                                                                                                           "status": {string},
     *                                                                                                                                                                                           "requestId": {object}
     *                                                                                                                                                                                         }
     *                                                                                                                                                                                         </pre>
     * @throws IOException if unable to deliver message
     */
    @Action(value = "notification/subscribe")
    @RolesAllowed({HiveRoles.ADMIN, HiveRoles.CLIENT, HiveRoles.KEY})
    @AllowedKeyAction(action = {GET_DEVICE_NOTIFICATION})
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
        User user = WebsocketSession.hasAuthorisedUser(session) ?
                WebsocketSession.getAuthorisedUser(session) :
                WebsocketSession.getAuthorizedAccessKey(session).getUser();
        List<DeviceNotification> deviceNotifications;
        if (user.getRole() == UserRole.ADMIN) {
            deviceNotifications =
                    deviceNotificationService.getDeviceNotificationList(null, user, timestamp, true);
        } else {
            deviceNotifications =
                    deviceNotificationService.getDeviceNotificationList(null, user, timestamp, false);
        }
        logger.debug(
                "notification/subscribe action - null guid case. get device notification. found {}  notifications. {}",
                deviceNotifications.size(), session.getId());
        notificationSubscribeAction(deviceNotifications, session, null);
    }

    private void prepareForNotificationSubscribeNotNullCase(List<String> guids, Session session, Timestamp timestamp)
            throws IOException {
        logger.debug("notification/subscribe action - null guid case. Session {}", session.getId());
        User authUser = WebsocketSession.getAuthorisedUser(session);
        AccessKey authKey = WebsocketSession.getAuthorizedAccessKey(session);
        User user = WebsocketSession.hasAuthorisedUser(session) ? authUser : authKey.getUser();

        List<Device> devices;
        checkPermissionsForDevices(timestamp, guids, ThreadLocalVariablesKeeper.getPrincipal());

        if (user.getRole() == UserRole.ADMIN) {
            devices = deviceDAO.findByUUID(guids);
        } else {
            devices = deviceDAO.findByUUIDListAndUser(user, guids);
        }
        if (devices.size() != guids.size()) {
            Set<String> foundDevicesGuids = new HashSet<>(devices.size());
            for (Device currentDevice : devices) {
                foundDevicesGuids.add(currentDevice.getGuid());
            }
            StringBuilder message = new StringBuilder("No access to devices with guids: {");
            for (String guid : guids) {
                if (!foundDevicesGuids.contains(guid)) {
                    message.append(guid).append(", ");
                }
            }
            message.delete(message.length() - 2, message.length());    //2 is a constant for string ", "
            // with length 2. We don't need extra commas and spaces.
            message.append("}");
            throw new HiveException(message.toString(), SC_NOT_FOUND);
        }
        if (devices.isEmpty()) {
            logger.debug("No devices found. Return " + ". Session {} ", session.getId());
            throw new HiveException("No available devices found.", SC_NOT_FOUND);
        }

        logger.debug("Found " + devices.size() + " devices" + ". Session " + session.getId());
        List<DeviceNotification> deviceNotifications =
                deviceNotificationService.getDeviceNotificationList(devices, user, timestamp, null);

        notificationSubscribeAction(deviceNotifications, session, devices);
        checkDevicesAndGuidsList(devices, guids, true);
    }

    private boolean checkPermissionsForDevices(Timestamp timestamp,
                                               List<String> guids,
                                               HivePrincipal principal) {
        if (principal.getUser() == null && principal.getKey() != null) {
            logger.debug("DeviceNotification poll was requested by Key = {}, timestamp = ",
                    principal.getKey().getId(), timestamp);
            if (!guids.isEmpty()) {
                List<String> guidsWithDeniedAccess = new LinkedList<>();
                for (String deviceGuid : guids) {
                    if (!accessKeyService.hasAccessToDevice(principal.getKey(), deviceGuid)) {
                        logger.debug("DeviceNotification poll requested by Key = {}, deviceId = {}, " +
                                "timestamp = cannot be proceed. No permissions to access device",
                                principal.getKey().getId(), deviceGuid, timestamp);
                        guidsWithDeniedAccess.add(deviceGuid);
                    }
                }
                if (!guidsWithDeniedAccess.isEmpty()) {
                    StringBuilder message = new StringBuilder("No access to devices with guids: {");
                    for (String guid : guidsWithDeniedAccess) {
                        message.append(guid).append(", ");
                    }
                    message.delete(message.length() - 2, message.length());    //2 is a constant for string ", "
                    // with length 2. We don't need extra commas and spaces.
                    message.append("}");
                    throw new HiveException(message.toString(), SC_UNAUTHORIZED);
                }
            }
        }
        return true;
    }

    private void notificationSubscribeAction(List<DeviceNotification> deviceNotifications, Session session,
                                             List<Device> devices)
            throws IOException {
        try {
            logger.debug("notification/subscribe action - not null guid case. found {} devices. Session {}",
                    deviceNotifications.size(), session.getId());
            WebsocketSession.getNotificationSubscriptionsLock(session).lock();

            User authUser = WebsocketSession.getAuthorisedUser(session);
            AccessKey authKey = WebsocketSession.getAuthorizedAccessKey(session);
            if (devices != null) {
                List<NotificationSubscription> nsList = new ArrayList<>();
                for (Device device : devices) {
                    boolean isAllowed = true;
                    if (authUser == null && authKey != null) {
                        isAllowed = accessKeyService.hasAccessToDevice(authKey, device.getGuid());
                    }
                    if (isAllowed) {
                        NotificationSubscription ns =
                                new NotificationSubscription(ThreadLocalVariablesKeeper.getPrincipal(), device.getId(),
                                        session.getId(),
                                        new WebsocketHandlerCreator(session, WebsocketSession.NOTIFICATIONS_LOCK,
                                                asyncMessageDeliverer));
                        nsList.add(ns);
                    }
                }
                subscriptionManager.getNotificationSubscriptionStorage().insertAll(nsList);
            } else {
                NotificationSubscription forAll =
                        new NotificationSubscription(ThreadLocalVariablesKeeper.getPrincipal(),
                                Constants.DEVICE_NOTIFICATION_NULL_ID_SUBSTITUTE,
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
     *
     * @param list    devices' guids list
     * @param session Current session
     * @return Json object with the following structure
     *         <pre>
     *                                                                                                                                                                                 {
     *                                                                                                                                                                                   "action": {string},
     *                                                                                                                                                                                   "status": {string},
     *                                                                                                                                                                                   "requestId": {object}
     *                                                                                                                                                                                 }
     *                                                                                                                                                                                 </pre>
     */
    @Action(value = "notification/unsubscribe")
    @RolesAllowed({HiveRoles.ADMIN, HiveRoles.CLIENT, HiveRoles.KEY})
    @AllowedKeyAction(action = {GET_DEVICE_NOTIFICATION})
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
                    throw new HiveException("No available devices found", SC_NOT_FOUND);
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
            throw new HiveException(responseBuilder.toString(), SC_UNAUTHORIZED);
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
     *                                                  {
     *                                                  "action": {string},
     *                                                  "status": {string},
     *                                                  "requestId": {object},
     *                                                  "info": {
     *                                                      "apiVersion": {string},
     *                                                      "serverTimestamp": {datetime},
     *                                                      "restServerUrl": {string}
     *                                                  }
     *                                                 }
     *                                                </pre>
     */

    @Action(value = "server/info")
    @PermitAll
    public WebSocketResponse processServerInfo(Session session) {
        logger.debug("server/info action started. Session " + session.getId());
        ApiInfo apiInfo = new ApiInfo();
        apiInfo.setApiVersion(Constants.API_VERSION);
        apiInfo.setServerTimestamp(timestampService.getTimestamp());
        String url = configurationService.get(Constants.REST_SERVER_URL);
        if (url != null) {
            apiInfo.setRestServerUrl(url);
        }
        WebSocketResponse response = new WebSocketResponse();
        response.addValue("info", apiInfo, WEBSOCKET_SERVER_INFO);
        logger.debug("server/info action completed. Session {}", session.getId());
        return response;
    }

    @Action("command/subscribe")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = {GET_DEVICE_COMMAND})
    public WebSocketResponse processCommandSubscribe(@WsParam(JsonMessageBuilder.TIMESTAMP) Timestamp timestamp,
                                                     @WsParam(JsonMessageBuilder.DEVICE_GUIDS) List<String> list,
                                                     Session session) throws IOException {
        logger.debug("command/subscribe requested for devices: {}. Timestamp: {}. Session: {}",
                list == null ? null : list.toString(), timestamp, session);
        if (timestamp == null) {
            timestamp = timestampService.getTimestamp();
        }

        if (list == null) {
            prepareForCommandsSubscribeNullCase(session, timestamp);
        } else if (!list.isEmpty()) {
            prepareForCommandsSubscribeNotNullCase(list, session, timestamp);
        }
        logger.debug("command/subscribe proceed successfully for devices: {}. Timestamp: {}. Session: {}",
                list == null ? null : list.toString(), timestamp, session);

        return new WebSocketResponse();
    }

    private void prepareForCommandsSubscribeNullCase(Session session, Timestamp timestamp) throws IOException {
        logger.debug("notification/subscribe action - null guid case. Session {}", session.getId());
        User user = WebsocketSession.hasAuthorisedUser(session) ?
                WebsocketSession.getAuthorisedUser(session) :
                WebsocketSession.getAuthorizedAccessKey(session).getUser();

        if (!WebsocketSession.hasAuthorisedUser(session)) {
            AccessKey key = WebsocketSession.getAuthorizedAccessKey(session);
            List<String> availableGuids = new ArrayList<>();
            Set<AccessKeyPermission> permissions = key.getPermissions();
            for (AccessKeyPermission currentPermission : permissions) {
                availableGuids.addAll(currentPermission.getDeviceGuidsAsSet());
            }
            if (!availableGuids.contains(null)) {
                prepareForCommandsSubscribeNotNullCase(availableGuids, session, timestamp);
                return;
            }
        }
        List<DeviceCommand> deviceCommands = commandService.getNewerThan(null, user, timestamp);
        logger.debug(
                "notification/subscribe action - null guid case. get device notification. found {}  notifications. {}",
                deviceCommands.size(), session.getId());
        commandsSubscribeAction(deviceCommands, session, null);
    }

    private void prepareForCommandsSubscribeNotNullCase(List<String> guids, Session session,
                                                        Timestamp timestamp) throws IOException {
        logger.debug("commands/subscribe action - null guid case. Session {}", session.getId());
        User authUser = WebsocketSession.getAuthorisedUser(session);
        AccessKey authKey = WebsocketSession.getAuthorizedAccessKey(session);
        User user = WebsocketSession.hasAuthorisedUser(session) ? authUser : authKey.getUser();

        checkPermissionsForDevices(timestamp, guids, ThreadLocalVariablesKeeper.getPrincipal());

        List<Device> devices;

        if (user.getRole() == UserRole.ADMIN) {
            devices = deviceDAO.findByUUID(guids);
        } else {
            devices = deviceDAO.findByUUIDListAndUser(user, guids);
        }
        if (devices.size() != guids.size()) {
            Set<String> foundDevicesGuids = new HashSet<>(devices.size());
            for (Device currentDevice : devices) {
                foundDevicesGuids.add(currentDevice.getGuid());
            }
            StringBuilder message = new StringBuilder("No access to devices with guids: {");
            for (String guid : guids) {
                if (!foundDevicesGuids.contains(guid)) {
                    message.append(guid).append(", ");
                }
            }
            message.delete(message.length() - 2, message.length());    //2 is a constant for string ", "
            // with length 2. We don't need extra commas and spaces.
            message.append("}");
            throw new HiveException(message.toString(), SC_NOT_FOUND);
        }
        if (devices.isEmpty()) {
            logger.debug("No devices found. Return " + ". Session {} ", session.getId());
            throw new HiveException("No available devices found.", SC_NOT_FOUND);
        }
        logger.debug("Found " + devices.size() + " devices" + ". Session " + session.getId());
        List<DeviceCommand> deviceCommands = commandService.getNewerThan(devices, timestamp);

        commandsSubscribeAction(deviceCommands, session, devices);
        checkDevicesAndGuidsList(devices, guids, true);
    }

    private void commandsSubscribeAction(List<DeviceCommand> deviceCommands, Session session,
                                         List<Device> devices)
            throws IOException {
        try {
            logger.debug("command/subscribe action - not null guid case. found {} devices. Session {}",
                    deviceCommands.size(), session.getId());
            WebsocketSession.getCommandsSubscriptionsLock(session).lock();

            User authUser = WebsocketSession.getAuthorisedUser(session);
            AccessKey authKey = WebsocketSession.getAuthorizedAccessKey(session);
            if (devices != null) {
                List<CommandSubscription> csList = new ArrayList<>();
                for (Device device : devices) {
                    boolean isAllowed = true;
                    if (authUser == null && authKey != null) {
                        isAllowed = accessKeyService.hasAccessToDevice(authKey, device.getGuid());
                    }
                    if (isAllowed) {
                        CommandSubscription cs =
                                new CommandSubscription(ThreadLocalVariablesKeeper.getPrincipal(), device.getId(),
                                        session.getId(),
                                        new WebsocketHandlerCreator(session,
                                                WebsocketSession.COMMANDS_SUBSCRIPTION_LOCK,
                                                asyncMessageDeliverer));
                        csList.add(cs);
                    }
                }
                subscriptionManager.getCommandSubscriptionStorage().insertAll(csList);
            } else {
                CommandSubscription forAll =
                        new CommandSubscription(ThreadLocalVariablesKeeper.getPrincipal(),
                                Constants.DEVICE_COMMAND_NULL_ID_SUBSTITUTE,
                                session.getId(),
                                new WebsocketHandlerCreator(session, WebsocketSession.COMMANDS_SUBSCRIPTION_LOCK,
                                        asyncMessageDeliverer));
                subscriptionManager.getCommandSubscriptionStorage().insert(forAll);
            }
            if (!deviceCommands.isEmpty()) {
                for (DeviceCommand deviceCommand : deviceCommands) {
                    WebsocketSession.addMessagesToQueue(session,
                            ServerResponsesFactory.createCommandInsertMessage(deviceCommand));
                }
            }
        } finally {
            WebsocketSession.getCommandsSubscriptionsLock(session).unlock();
            logger.debug("deliver messages process for session" + session.getId());
            asyncMessageDeliverer.deliverMessages(session);
        }
    }

    @Action("command/unsubscribe")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = {GET_DEVICE_COMMAND})
    public WebSocketResponse processCommandUnsubscribe(@WsParam(JsonMessageBuilder.DEVICE_GUIDS) List<String> list,
                                                       Session session) {
        logger.debug("command/unsubscribe action. Session {} ", session.getId());
        try {
            WebsocketSession.getCommandsSubscriptionsLock(session).lock();
            List<Device> devices = null;
            List<Pair<Long, String>> subs;
            if (list != null && !list.isEmpty()) {
                devices = deviceDAO.findByUUID(list);
                logger.debug("command/unsubscribe. found {} devices. ", devices.size());
                if (devices.isEmpty()) {
                    throw new HiveException("No available devices found", SC_NOT_FOUND);
                }

                logger.debug("command/unsubscribe. performing unsubscribing action");

                subs = new ArrayList<>(devices.size());
                for (Device device : devices) {
                    subs.add(ImmutablePair.of(device.getId(), session.getId()));
                }
            } else {
                subs = new ArrayList<>(1);
                subs.add(ImmutablePair.of(Constants.DEVICE_COMMAND_NULL_ID_SUBSTITUTE, session.getId()));

            }
            subscriptionManager.getCommandSubscriptionStorage().removePairs(subs);
            checkDevicesAndGuidsList(devices, list, false);
        } finally {
            WebsocketSession.getCommandsSubscriptionsLock(session).unlock();
        }

        logger.debug("command/unsubscribe completed for session {}", session.getId());
        return new WebSocketResponse();
    }

    @Action("command/update")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = {UPDATE_DEVICE_COMMAND})
    public WebSocketResponse processCommandUpdate(@WsParam(JsonMessageBuilder.DEVICE_GUID) String guid,
                                                  @WsParam(JsonMessageBuilder.COMMAND_ID) Long id,
                                                  @WsParam(JsonMessageBuilder.COMMAND)
                                                  @JsonPolicyDef(REST_COMMAND_UPDATE_FROM_DEVICE)
                                                  DeviceCommandUpdate commandUpdate,
                                                  Session session) {
        logger.debug("command/update requested for session: {}. Device guid: {}. Command id: {}", session, guid, id);

        if (guid == null || id == null) {
            logger.debug("command/update canceled for session: {}. Guid or command id is not provided", session);
            throw new HiveException("Device guid and command id are required parameters!", SC_BAD_REQUEST);
        }
        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        User user = principal.getUser();
        if (user == null && principal.getKey() != null) {
            user = principal.getKey().getUser();
            if (!accessKeyService.hasAccessToDevice(principal.getKey(), guid)) {
                logger.debug("No permissions to access device with guid {} for access key with id {}", guid,
                        principal.getKey().getId());
                throw new HiveException("Device with such guid not fount!", SC_NOT_FOUND);
            }
        }
        Device device = deviceService.getDevice(guid, user, principal.getDevice());
        if (commandUpdate == null) {
            throw new HiveException("command with id " + id + " for device with " + guid + " is not found",
                    SC_NOT_FOUND);
        }
        commandUpdate.setId(id);
        commandService.submitDeviceCommandUpdate(commandUpdate, device);

        logger.debug("command/update proceed successfully for session: {}. Device guid: {}. Command id: {}", session,
                guid, id);
        return new WebSocketResponse();
    }

    @Action("notification/insert")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = {CREATE_DEVICE_NOTIFICATION})
    public WebSocketResponse processNotificationInsert(@WsParam(JsonMessageBuilder.DEVICE_GUID) String deviceGuid,
                                                       @WsParam(JsonMessageBuilder.NOTIFICATION)
                                                       @JsonPolicyDef(NOTIFICATION_FROM_DEVICE)
                                                       DeviceNotification notification,
                                                       Session session) {
        logger.debug("notification/insert requested. Session {}. Guid {}", session, deviceGuid);
        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        User user = principal.getUser();
        if (user == null && principal.getKey() != null) {
            user = principal.getKey().getUser();
            if (!accessKeyService.hasAccessToDevice(principal.getKey(), deviceGuid)) {
                logger.debug("No device found with guid : {} for access key {}", deviceGuid,
                        principal.getKey().getId());
                throw new HiveException("No device found with guid : " + deviceGuid, SC_NOT_FOUND);
            }
        }
        if (notification == null || notification.getNotification() == null) {
            logger.debug(
                    "notification/insert proceed with error. Bad notification: notification is required.");
            throw new HiveException("Notification is required!", SC_BAD_REQUEST);
        }

        Device device = deviceService.getDevice(deviceGuid, user, principal.getDevice());
        if (device.getNetwork() == null) {
            logger.debug(
                    "notification/insert. No network specified for device with guid = {}", deviceGuid);
            throw new HiveException("No access to device!", SC_FORBIDDEN);
        }
        deviceNotificationService.submitDeviceNotification(notification, device);
        logger.debug("notification/insert proceed successfully. Session {}. Guid {}", session, deviceGuid);

        WebSocketResponse response = new WebSocketResponse();
        response.addValue(JsonMessageBuilder.NOTIFICATION, notification, NOTIFICATION_TO_DEVICE);
        return response;
    }

}