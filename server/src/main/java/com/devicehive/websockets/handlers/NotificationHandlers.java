package com.devicehive.websockets.handlers;


import com.devicehive.auth.AllowedKeyAction;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.messages.handler.WebsocketHandlerCreator;
import com.devicehive.messages.subscriptions.NotificationSubscription;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.SubscriptionFilterInternal;
import com.devicehive.service.DeviceNotificationService;
import com.devicehive.service.DeviceService;
import com.devicehive.service.TimestampService;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.ServerResponsesFactory;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.devicehive.websockets.handlers.annotations.Action;
import com.devicehive.websockets.handlers.annotations.WebsocketController;
import com.devicehive.websockets.handlers.annotations.WsParam;
import com.devicehive.websockets.util.AsyncMessageSupplier;
import com.devicehive.websockets.util.SubscriptionSessionMap;
import com.devicehive.websockets.util.WebsocketSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.websocket.Session;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.devicehive.auth.AllowedKeyAction.Action.CREATE_DEVICE_NOTIFICATION;
import static com.devicehive.auth.AllowedKeyAction.Action.GET_DEVICE_NOTIFICATION;
import static com.devicehive.configuration.Constants.DEVICE_GUID;
import static com.devicehive.configuration.Constants.DEVICE_GUIDS;
import static com.devicehive.configuration.Constants.NAMES;
import static com.devicehive.configuration.Constants.NOTIFICATION;
import static com.devicehive.configuration.Constants.SUBSCRIPTION_ID;
import static com.devicehive.configuration.Constants.TIMESTAMP;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_FROM_DEVICE;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_TO_DEVICE;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;


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
    @EJB
    private SubscriptionSessionMap subscriptionSessionMap;

    @Action(value = "notification/subscribe")
    @RolesAllowed({HiveRoles.ADMIN, HiveRoles.CLIENT, HiveRoles.DEVICE, HiveRoles.KEY})
    @AllowedKeyAction(action = {GET_DEVICE_NOTIFICATION})
    public WebSocketResponse processNotificationSubscribe(@WsParam(TIMESTAMP) Timestamp timestamp,
                                                          @WsParam(DEVICE_GUIDS)
                                                          List<String> devices,
                                                          @WsParam(NAMES) List<String> names,
                                                          @WsParam(DEVICE_GUID) String deviceId,
                                                          Session session) throws IOException {
        logger.debug("notification/subscribe requested for devices: {}, {}. Timestamp: {}. Names {} Session: {}",
                devices, deviceId, timestamp, names, session);
        devices = prepareActualList(devices, deviceId);
        UUID subId = notificationSubscribeAction(session, devices, names, timestamp);
        logger.debug("notification/subscribe done for devices: {}, {}. Timestamp: {}. Names {} Session: {}",
                devices, deviceId, timestamp, names, session);
        WebSocketResponse response = new WebSocketResponse();
        response.addValue(SUBSCRIPTION_ID, subId, null);
        return response;

    }

    private List<String> prepareActualList(List<String> deviceIdList, String deviceId) {
        if (deviceId == null && deviceIdList == null) {
            return null;
        }
        List<String> actualList = new ArrayList<>();
        if (deviceIdList != null) {
            actualList.addAll(deviceIdList);
        }
        if (deviceId != null) {
            actualList.add(deviceId);
        }
        return actualList;
    }

    private UUID notificationSubscribeAction(Session session,
                                             List<String> devices,
                                             List<String> names,
                                             Timestamp timestamp) throws IOException {
        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        try {
            logger.debug("notification/subscribe action. Session {}", session.getId());
            WebsocketSession.getNotificationSubscriptionsLock(session).lock();
            List<NotificationSubscription> nsList = new ArrayList<>();
            SubscriptionFilterInternal sfi;
            UUID reqId = UUID.randomUUID();
            if (devices != null) {
                List<Device> actualDevices = deviceService.findByGuidWithPermissionsCheck(devices, principal);
                for (Device d : actualDevices) {
                    nsList.add(new NotificationSubscription(principal, d.getId(),
                            reqId,
                            names,
                            new WebsocketHandlerCreator(session,
                                    WebsocketSession.NOTIFICATION_SUBSCRIPTION_LOCK)
                    ));
                }
                sfi = SubscriptionFilterInternal.createForManyDevices(devices, timestamp);
            } else {
                NotificationSubscription forAll =
                        new NotificationSubscription(principal,
                                Constants.NULL_ID_SUBSTITUTE,
                                reqId,
                                names,
                                new WebsocketHandlerCreator(session, WebsocketSession.NOTIFICATION_SUBSCRIPTION_LOCK)
                        );
                nsList.add(forAll);
                sfi = SubscriptionFilterInternal.createForAllDevices(null, timestamp);
            }
            subscriptionSessionMap.put(reqId, session);
            WebsocketSession.getNotificationSubscriptions(session).put(reqId, sfi);
            subscriptionManager.getNotificationSubscriptionStorage().insertAll(nsList);
            if (timestamp == null) {
                timestamp = timestampService.getTimestamp();
            }
            List<DeviceNotification> notifications =
                    deviceNotificationService.getDeviceNotificationList(devices, names, timestamp, principal);
            if (!notifications.isEmpty()) {
                for (DeviceNotification notification : notifications) {
                    WebsocketSession.addMessagesToQueue(session,
                            ServerResponsesFactory.createNotificationInsertMessage(notification));
                }
            }
            return reqId;
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
    public WebSocketResponse processNotificationUnsubscribe(Session session,
                                                            @WsParam(SUBSCRIPTION_ID) UUID subId,
                                                            @WsParam(DEVICE_GUIDS) List<String>
                                                                    deviceGuids) {
        logger.debug("notification/unsubscribe action. Session {} ", session.getId());
        try {
            WebsocketSession.getNotificationSubscriptionsLock(session).lock();
            if (subId == null) {
                List<UUID> subIds = new ArrayList<>();
                SubscriptionFilterInternal oldSubsriptionFormatToUnsubscribe;
                if (deviceGuids != null) {
                    oldSubsriptionFormatToUnsubscribe =
                            SubscriptionFilterInternal.createForManyDevices(deviceGuids, null);
                } else {
                    oldSubsriptionFormatToUnsubscribe = SubscriptionFilterInternal.createForAllDevices(null, null);
                }
                for (Map.Entry<UUID, SubscriptionFilterInternal> currentSubscription :
                        WebsocketSession.getNotificationSubscriptions(session).entrySet()) {
                    if (currentSubscription.getValue().equals(oldSubsriptionFormatToUnsubscribe)) {
                        subIds.add(currentSubscription.getKey());


                    }
                }
            } else {
                if (WebsocketSession.getNotificationSubscriptions(session).containsKey(subId)) {
                    WebsocketSession.getNotificationSubscriptions(session).remove(subId);
                    subscriptionSessionMap.remove(subId);
                    subscriptionManager.getNotificationSubscriptionStorage().removeBySubscriptionId(subId);
                }
            }
        } finally {
            WebsocketSession.getNotificationSubscriptionsLock(session).unlock();
            logger.debug("deliver messages process for session" + session.getId());
            asyncMessageDeliverer.deliverMessages(session);
        }

        logger.debug("notification/unsubscribe completed for session {}", session.getId());
        return new WebSocketResponse();
    }

    @Action("notification/insert")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.DEVICE, HiveRoles.KEY})
    @AllowedKeyAction(action = {CREATE_DEVICE_NOTIFICATION})
    public WebSocketResponse processNotificationInsert(@WsParam(DEVICE_GUID) String deviceGuid,
                                                       @WsParam(NOTIFICATION)
                                                       @JsonPolicyApply(NOTIFICATION_FROM_DEVICE)
                                                       DeviceNotification notification,
                                                       Session session) {
        logger.debug("notification/insert requested. Session {}. Guid {}", session, deviceGuid);
        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        if (notification == null || notification.getNotification() == null) {
            logger.debug(
                    "notification/insert proceed with error. Bad notification: notification is required.");
            throw new HiveException(Messages.NOTIFICATION_REQUIRED, SC_BAD_REQUEST);
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
            throw new HiveException(Messages.DEVICE_IS_NOT_CONNECTED_TO_NETWORK, SC_FORBIDDEN);
        }
        deviceNotificationService.submitDeviceNotification(notification, device);
        logger.debug("notification/insert proceed successfully. Session {}. Guid {}", session, deviceGuid);

        WebSocketResponse response = new WebSocketResponse();
        response.addValue(NOTIFICATION, notification, NOTIFICATION_TO_DEVICE);
        return response;
    }


}
