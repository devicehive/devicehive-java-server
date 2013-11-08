package com.devicehive.websockets.handlers;


import com.devicehive.auth.AllowedKeyAction;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.Constants;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.messages.handler.WebsocketHandlerCreator;
import com.devicehive.messages.subscriptions.NotificationSubscription;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.User;
import com.devicehive.service.DeviceNotificationService;
import com.devicehive.service.DeviceService;
import com.devicehive.service.TimestampService;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.ServerResponsesFactory;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import com.devicehive.websockets.converters.JsonMessageBuilder;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.devicehive.websockets.handlers.annotations.Action;
import com.devicehive.websockets.handlers.annotations.WebsocketController;
import com.devicehive.websockets.handlers.annotations.WsParam;
import com.devicehive.websockets.util.AsyncMessageSupplier;
import com.devicehive.websockets.util.WebsocketSession;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.websocket.Session;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.devicehive.auth.AllowedKeyAction.Action.CREATE_DEVICE_NOTIFICATION;
import static com.devicehive.auth.AllowedKeyAction.Action.GET_DEVICE_NOTIFICATION;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_FROM_DEVICE;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_TO_DEVICE;
import static javax.servlet.http.HttpServletResponse.*;


@WebsocketController
@LogExecutionTime
public class NotificationHandlers implements WebsocketHandlers {

    private static final Logger logger = LoggerFactory.getLogger(NotificationHandlers.class);
    @EJB
    private SubscriptionManager subscriptionManager;
    @EJB
    private DeviceService deviceService;
    @EJB
    private DeviceNotificationService deviceNotificationService;
    @EJB
    private AsyncMessageSupplier asyncMessageDeliverer;
    @EJB
    private TimestampService timestampService;

    @Action(value = "notification/subscribe")
    @RolesAllowed({HiveRoles.ADMIN, HiveRoles.CLIENT, HiveRoles.DEVICE, HiveRoles.KEY})
    @AllowedKeyAction(action = {GET_DEVICE_NOTIFICATION})
    public WebSocketResponse processNotificationSubscribe(@WsParam(JsonMessageBuilder.DEVICE_GUIDS) List<String> list,
                                                          @WsParam(JsonMessageBuilder.NOTIFICATION_NAMES)
                                                          List<String> names,
                                                          @WsParam(JsonMessageBuilder.TIMESTAMP) Timestamp timestamp,
                                                          Session session) throws IOException {
        logger.debug("notification/subscribe action. Session {} ", session.getId());
        if (timestamp == null) {
            timestamp = timestampService.getTimestamp();
        }
        if (list == null || list.isEmpty()) {
            prepareForNotificationSubscribeNullCase(names, session, timestamp);
        } else {
            prepareForNotificationSubscribeNotNullCase(list, names, session, timestamp);
        }
        logger.debug("notification/subscribe action  finished");
        return new WebSocketResponse();

    }

    private void prepareForNotificationSubscribeNullCase(List<String> names, Session session, Timestamp timestamp)
            throws IOException {
        logger.debug("notification/subscribe action - null guid case. Session {}", session.getId());
        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        User user = principal.getUser();
        if (user == null) {
            user = principal.getKey().getUser();
        }
        List<DeviceNotification> deviceNotifications = deviceNotificationService.getDeviceNotificationList(null,
                names, user, timestamp);
        logger.debug(
                "notification/subscribe action - null guid case. get device notification. found {}  notifications. {}",
                deviceNotifications.size(), session.getId());
        notificationSubscribeAction(deviceNotifications, session, null, names);
    }

    private void prepareForNotificationSubscribeNotNullCase(List<String> guids, List<String> names,
                                                            Session session, Timestamp timestamp)
            throws IOException {
        logger.debug("notification/subscribe action - null guid case. Session {}", session.getId());
        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        List<Device> devices = deviceService.findByGuidWithPermissionsCheck(guids, principal);
        if (devices.size() != guids.size()) {
            String message = CommandHandlers.createAccessDeniedForGuidsMessage(guids, devices);
            throw new HiveException(message, SC_NOT_FOUND);
        }
        logger.debug("Found " + devices.size() + " devices" + ". Session " + session.getId());
        User user = principal.getUser();
        if (user == null)
            user = principal.getKey().getUser();
        List<DeviceNotification> deviceNotifications =
                deviceNotificationService.getDeviceNotificationList(devices, names, user, timestamp);
        notificationSubscribeAction(deviceNotifications, session, devices, names);
//        checkDevicesAndGuidsList(devices, guids, true);
    }

    private void notificationSubscribeAction(List<DeviceNotification> deviceNotifications, Session session,
                                             List<Device> devices, Collection<String> names)
            throws IOException {
        try {
            logger.debug("notification/subscribe action - not null guid case. found {} devices. Session {}",
                    deviceNotifications.size(), session.getId());
            WebsocketSession.getNotificationSubscriptionsLock(session).lock();
            if (devices != null) {
                List<NotificationSubscription> nsList = new ArrayList<>();
                for (Device device : devices) {
                    NotificationSubscription ns =
                            new NotificationSubscription(ThreadLocalVariablesKeeper.getPrincipal(), device.getId(),
                                    session.getId(), names,
                                    new WebsocketHandlerCreator(session, WebsocketSession.NOTIFICATIONS_LOCK,
                                            asyncMessageDeliverer));
                    nsList.add(ns);

                }
                subscriptionManager.getNotificationSubscriptionStorage().insertAll(nsList);
            } else {
                NotificationSubscription forAll =
                        new NotificationSubscription(ThreadLocalVariablesKeeper.getPrincipal(),
                                Constants.DEVICE_NOTIFICATION_NULL_ID_SUBSTITUTE,
                                session.getId(), names,
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
     *         <code>
     *         {
     *         "action": {string},
     *         "status": {string},
     *         "requestId": {object}
     *         }
     *         </code>
     */
    @Action(value = "notification/unsubscribe")
    @RolesAllowed({HiveRoles.ADMIN, HiveRoles.CLIENT, HiveRoles.DEVICE, HiveRoles.KEY})
    @AllowedKeyAction(action = {GET_DEVICE_NOTIFICATION})
    public WebSocketResponse processNotificationUnsubscribe(@WsParam(JsonMessageBuilder.DEVICE_GUIDS) List<String> list,
                                                            Session session) {
        logger.debug("notification/unsubscribe action. Session {} ", session.getId());
        try {
            WebsocketSession.getNotificationSubscriptionsLock(session).lock();
            List<Pair<Long, String>> subs;
            if (list != null && !list.isEmpty()) {
                HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
                List<Device> devices = deviceService.findByGuidWithPermissionsCheck(list, principal);
                logger.debug("notification/unsubscribe. found {} devices. ", devices.size());
                if (devices.size() != list.size()) {
                    String message = CommandHandlers.createAccessDeniedForGuidsMessage(list, devices);
                    throw new HiveException(message, SC_NOT_FOUND);
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
        } finally {
            WebsocketSession.getNotificationSubscriptionsLock(session).unlock();
        }

        logger.debug("notification/unsubscribe completed for session {}", session.getId());
        return new WebSocketResponse();
    }

    @Action("notification/insert")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.DEVICE, HiveRoles.KEY})
    @AllowedKeyAction(action = {CREATE_DEVICE_NOTIFICATION})
    public WebSocketResponse processNotificationInsert(@WsParam(JsonMessageBuilder.DEVICE_GUID) String deviceGuid,
                                                       @WsParam(JsonMessageBuilder.NOTIFICATION)
                                                       @JsonPolicyApply(NOTIFICATION_FROM_DEVICE)
                                                       DeviceNotification notification,
                                                       Session session) {
        logger.debug("notification/insert requested. Session {}. Guid {}", session, deviceGuid);
        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        if (notification == null || notification.getNotification() == null) {
            logger.debug(
                    "notification/insert proceed with error. Bad notification: notification is required.");
            throw new HiveException("Notification is required!", SC_BAD_REQUEST);
        }
        Device device;
        if (deviceGuid == null) {
            device = principal.getDevice();
        } else {
            device = deviceService.findByGuidWithPermissionsCheck(deviceGuid, principal);
        }
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
