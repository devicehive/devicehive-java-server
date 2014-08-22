package com.devicehive.websockets.handlers;


import com.devicehive.auth.AllowedKeyAction;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.auth.HiveSecurityContext;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.messages.handler.WebsocketHandlerCreator;
import com.devicehive.messages.subscriptions.NotificationSubscription;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceNotification;
import com.devicehive.service.DeviceNotificationService;
import com.devicehive.service.DeviceService;
import com.devicehive.service.TimestampService;
import com.devicehive.util.ServerResponsesFactory;
import com.devicehive.websockets.HiveWebsocketSessionState;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.devicehive.websockets.handlers.annotations.Action;
import com.devicehive.websockets.handlers.annotations.WsParam;
import com.devicehive.websockets.util.AsyncMessageSupplier;
import com.devicehive.websockets.util.FlushQueue;
import com.devicehive.websockets.util.SubscriptionSessionMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.websocket.Session;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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


public class NotificationHandlers extends WebsocketHandlers {

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

    @Inject
    private HiveSecurityContext hiveSecurityContext;

    @Inject
    @FlushQueue
    private Event<Session> event;

    @Action(value = "notification/subscribe")
    @RolesAllowed({HiveRoles.ADMIN, HiveRoles.CLIENT,  HiveRoles.KEY})
    @AllowedKeyAction(action = GET_DEVICE_NOTIFICATION)
    public WebSocketResponse processNotificationSubscribe(@WsParam(TIMESTAMP) Timestamp timestamp,
                                                          @WsParam(DEVICE_GUIDS)
                                                          Set<String> devices,
                                                          @WsParam(NAMES) Set<String> names,
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

    private Set<String> prepareActualList(Set<String> deviceIdSet, final String deviceId) {
        if (deviceId == null && deviceIdSet == null) {
            return null;
        }
        if (deviceIdSet != null && deviceId == null) {
            deviceIdSet.remove(null);
            return deviceIdSet;
        }
        if (deviceIdSet == null) {
            return new HashSet<String>() {{
                add(deviceId);
            }};
        }
        throw new HiveException(Messages.INVALID_REQUEST_PARAMETERS, SC_BAD_REQUEST);
    }

    private UUID notificationSubscribeAction(Session session,
                                             Set<String> devices,
                                             Set<String> names,
                                             Timestamp timestamp) throws IOException {
        HivePrincipal principal = hiveSecurityContext.getHivePrincipal();
        if (timestamp == null) {
            timestamp = timestampService.getTimestamp();
        }
        if (names != null && (names.isEmpty() || (names.size() == 1 && names.contains(null)))) {
            throw new HiveException(Messages.EMPTY_NAMES, SC_BAD_REQUEST);
        }
        HiveWebsocketSessionState state = HiveWebsocketSessionState.get(session);
        state.getNotificationSubscriptionsLock().lock();
        try {
            logger.debug("notification/subscribe action. Session {}", session.getId());
            List<NotificationSubscription> nsList = new ArrayList<>();
            UUID reqId = UUID.randomUUID();
            if (devices != null) {
                List<Device> actualDevices = deviceService.findByGuidWithPermissionsCheck(devices, principal);
                for (Device d : actualDevices) {
                    nsList.add(new NotificationSubscription(principal, d.getId(),
                            reqId,
                            names,
                            WebsocketHandlerCreator.createNotificationInsert(session)
                    ));
                }
            } else {
                NotificationSubscription forAll =
                        new NotificationSubscription(principal,
                                Constants.NULL_ID_SUBSTITUTE,
                                reqId,
                                names,
                                WebsocketHandlerCreator.createNotificationInsert(session)
                        );
                nsList.add(forAll);
            }
            subscriptionSessionMap.put(reqId, session);
            if (names == null) {
                state.addOldFormatNotificationSubscription(devices, reqId);
            }
            state.getNotificationSubscriptions().add(reqId);
            subscriptionManager.getNotificationSubscriptionStorage().insertAll(nsList);
            if (timestamp == null) {
                timestamp = timestampService.getTimestamp();
            }
            List<DeviceNotification> notifications =
                    deviceNotificationService.getDeviceNotificationList(devices, names, timestamp, principal);
            if (!notifications.isEmpty()) {
                for (DeviceNotification notification : notifications) {
                    state.getQueue().add(ServerResponsesFactory.createNotificationInsertMessage(notification, reqId));
                }
            }
            return reqId;
        } finally {
            state.getNotificationSubscriptionsLock().unlock();
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
    @RolesAllowed({HiveRoles.ADMIN, HiveRoles.CLIENT, HiveRoles.KEY})
    @AllowedKeyAction(action = GET_DEVICE_NOTIFICATION)
    public WebSocketResponse processNotificationUnsubscribe(Session session,
                                                            @WsParam(SUBSCRIPTION_ID) UUID subId,
                                                            @WsParam(DEVICE_GUIDS) Set<String> deviceGuids) {
        logger.debug("notification/unsubscribe action. Session {} ", session.getId());
        HiveWebsocketSessionState state = HiveWebsocketSessionState.get(session);
        state.getNotificationSubscriptionsLock().lock();
        try {
            Set<UUID> subscriptions = new HashSet<>();
            if (subId == null) {
                if (deviceGuids == null) {
                    Set<String> subForAll = new HashSet<String>() {{
                        add(Constants.NULL_SUBSTITUTE);
                    }};
                    subscriptions.addAll(state.removeOldFormatNotificationSubscription(subForAll));
                } else
                    subscriptions.addAll(state.removeOldFormatNotificationSubscription(deviceGuids));
            } else {
                subscriptions.add(subId);
            }
            for (UUID toUnsubscribe : subscriptions) {
                if (state.getNotificationSubscriptions().contains(toUnsubscribe)) {
                    state.getNotificationSubscriptions().remove(toUnsubscribe);
                    subscriptionSessionMap.remove(toUnsubscribe);
                    subscriptionManager.getNotificationSubscriptionStorage().removeBySubscriptionId(toUnsubscribe);
                }
            }
        } finally {
            state.getNotificationSubscriptionsLock().unlock();
            logger.debug("deliver messages process for session" + session.getId());
            event.fire(session);
        }
        logger.debug("notification/unsubscribe completed for session {}", session.getId());
        return new WebSocketResponse();
    }

    @Action("notification/insert")
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.DEVICE, HiveRoles.KEY})
    @AllowedKeyAction(action = CREATE_DEVICE_NOTIFICATION)
    public WebSocketResponse processNotificationInsert(@WsParam(DEVICE_GUID) String deviceGuid,
                                                       @WsParam(NOTIFICATION)
                                                       @JsonPolicyApply(NOTIFICATION_FROM_DEVICE)
                                                       DeviceNotification notification,
                                                       Session session) {
        logger.debug("notification/insert requested. Session {}. Guid {}", session, deviceGuid);
        HivePrincipal principal = hiveSecurityContext.getHivePrincipal();
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
        if (device == null){
            logger.debug("notification/insert canceled for session: {}. Guid is not provided", session);
            throw new HiveException(Messages.DEVICE_GUID_REQUIRED, SC_FORBIDDEN);
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
