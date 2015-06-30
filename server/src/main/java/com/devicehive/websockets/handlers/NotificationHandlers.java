package com.devicehive.websockets.handlers;


import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.messages.handler.WebsocketHandlerCreator;
import com.devicehive.messages.subscriptions.NotificationSubscription;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.wrappers.DeviceNotificationWrapper;
import com.devicehive.service.DeviceNotificationService;
import com.devicehive.service.DeviceService;
import com.devicehive.util.ServerResponsesFactory;
import com.devicehive.websockets.HiveWebsocketSessionState;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.devicehive.websockets.handlers.annotations.Action;
import com.devicehive.websockets.handlers.annotations.WsParam;
import com.devicehive.websockets.util.AsyncMessageSupplier;
import com.devicehive.websockets.util.SubscriptionSessionMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;

import static com.devicehive.configuration.Constants.*;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_FROM_DEVICE;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_TO_DEVICE;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;

@Component
public class NotificationHandlers extends WebsocketHandlers {
    private static final Logger logger = LoggerFactory.getLogger(NotificationHandlers.class);

    @Autowired
    private SubscriptionManager subscriptionManager;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private AsyncMessageSupplier asyncMessageDeliverer;
    @Autowired
    private SubscriptionSessionMap subscriptionSessionMap;
    @Autowired
    private DeviceNotificationService notificationService;
    @Autowired
    private AsyncMessageSupplier messageSupplier;

    @Action(value = "notification/subscribe")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT', 'KEY') and hasPermission(null, 'GET_DEVICE_NOTIFICATION')")
    public WebSocketResponse processNotificationSubscribe(@WsParam(TIMESTAMP) Date timestamp,
                                                          @WsParam(DEVICE_GUIDS) Set<String> devices,
                                                          @WsParam(NAMES) Set<String> names,
                                                          @WsParam(DEVICE_GUID) String deviceId,
                                                          WebSocketSession session) throws IOException {
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
            return new HashSet<String>() {
                {
                    add(deviceId);
                }

                private static final long serialVersionUID = 955343867580964077L;
            };

        }
        throw new HiveException(Messages.INVALID_REQUEST_PARAMETERS, SC_BAD_REQUEST);
    }

    private UUID notificationSubscribeAction(WebSocketSession session,
                                             Set<String> devices,
                                             Set<String> names,
                                             Date timestamp) throws IOException {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
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
                    nsList.add(new NotificationSubscription(principal, d.getGuid(), reqId, StringUtils.join(names, ","),
                                                            WebsocketHandlerCreator.createNotificationInsert(session)));
                }
            } else {
                NotificationSubscription forAll =
                    new NotificationSubscription(principal, Constants.NULL_SUBSTITUTE, reqId, StringUtils.join(names, ","),
                                                 WebsocketHandlerCreator.createNotificationInsert(session));
                nsList.add(forAll);
            }
            subscriptionSessionMap.put(reqId, session);
            if (names == null) {
                state.addOldFormatNotificationSubscription(devices, reqId);
            }
            state.getNotificationSubscriptions().add(reqId);
            subscriptionManager.getNotificationSubscriptionStorage().insertAll(nsList);

            if (timestamp != null) {
                Collection<DeviceNotification> notifications = notificationService.find(null, null,
                        devices, names, timestamp, Constants.DEFAULT_TAKE, principal);
                if (!notifications.isEmpty()) {
                    for (DeviceNotification deviceNotification : notifications) {
                        state.getQueue().add(ServerResponsesFactory.createNotificationInsertMessage(deviceNotification, reqId));
                    }
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
     * WebSocket API: Client: notification/unsubscribe</a> Unsubscribes from device notifications.
     *
     * @param session Current session
     * @return Json object with the following structure <code> { "action": {string}, "status": {string}, "requestId":
     *         {object} } </code>
     */
    @Action(value = "notification/unsubscribe")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT', 'KEY') and hasPermission(null, 'GET_DEVICE_NOTIFICATION')")
    public WebSocketResponse processNotificationUnsubscribe(WebSocketSession session,
                                                            @WsParam(SUBSCRIPTION_ID) UUID subId,
                                                            @WsParam(DEVICE_GUIDS) Set<String> deviceGuids) {
        logger.debug("notification/unsubscribe action. Session {} ", session.getId());
        HiveWebsocketSessionState state = HiveWebsocketSessionState.get(session);
        state.getNotificationSubscriptionsLock().lock();
        try {
            Set<UUID> subscriptions = new HashSet<>();
            if (subId == null) {
                if (deviceGuids == null) {
                    Set<String> subForAll = new HashSet<String>() {
                        {
                            add(Constants.NULL_SUBSTITUTE);
                        }

                        private static final long serialVersionUID = 2484204746448211456L;
                    };
                    subscriptions.addAll(state.removeOldFormatNotificationSubscription(subForAll));
                } else {
                    subscriptions.addAll(state.removeOldFormatNotificationSubscription(deviceGuids));
                }
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
            messageSupplier.deliverMessages(session);
        }
        logger.debug("notification/unsubscribe completed for session {}", session.getId());
        return new WebSocketResponse();
    }

    @Action("notification/insert")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT', 'KEY', 'DEVICE') and hasPermission(null, 'CREATE_DEVICE_NOTIFICATION')")
    public WebSocketResponse processNotificationInsert(@WsParam(DEVICE_GUID) String deviceGuid,
                                                       @WsParam(NOTIFICATION)
                                                       @JsonPolicyDef(NOTIFICATION_FROM_DEVICE)
                                                       DeviceNotificationWrapper notificationSubmit,
                                                       WebSocketSession session) {
        logger.debug("notification/insert requested. Session {}. Guid {}", session, deviceGuid);
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (notificationSubmit == null || notificationSubmit.getNotification() == null) {
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
        if (device == null) {
            logger.debug("notification/insert canceled for session: {}. Guid is not provided", session);
            throw new HiveException(Messages.DEVICE_GUID_REQUIRED, SC_FORBIDDEN);
        }
        if (device.getNetwork() == null) {
            logger.debug("notification/insert. No network specified for device with guid = {}", deviceGuid);
            throw new HiveException(String.format(Messages.DEVICE_IS_NOT_CONNECTED_TO_NETWORK, deviceGuid), SC_FORBIDDEN);
        }
        DeviceNotification message = notificationService.convertToMessage(notificationSubmit, device);
        notificationService.submitDeviceNotification(message, device);
        logger.debug("notification/insert proceed successfully. Session {}. Guid {}", session, deviceGuid);

        WebSocketResponse response = new WebSocketResponse();
        response.addValue(NOTIFICATION, message, NOTIFICATION_TO_DEVICE);
        return response;
    }
}
