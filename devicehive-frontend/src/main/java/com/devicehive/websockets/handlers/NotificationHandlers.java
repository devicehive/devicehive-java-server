package com.devicehive.websockets.handlers;

import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.websockets.InsertNotification;
import com.devicehive.model.wrappers.DeviceNotificationWrapper;
import com.devicehive.resource.util.JsonTypes;
import com.devicehive.service.DeviceNotificationService;
import com.devicehive.service.DeviceService;
import com.devicehive.util.ServerResponsesFactory;
import com.devicehive.vo.DeviceVO;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiConsumer;

import static com.devicehive.configuration.Constants.*;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_TO_DEVICE;
import static com.devicehive.messages.handler.WebSocketClientHandler.sendMessage;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

@Component
public class NotificationHandlers {
    private static final Logger logger = LoggerFactory.getLogger(NotificationHandlers.class);

    public static final String SUBSCSRIPTION_SET_NAME = "notificationSubscriptions";

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceNotificationService notificationService;

    @Autowired
    private Gson gson;


    @PreAuthorize("hasAnyAuthority('ADMIN', 'CLIENT', 'KEY') and hasPermission(null, 'GET_DEVICE_NOTIFICATION')")
    public WebSocketResponse processNotificationSubscribe(JsonObject request,
                                                          WebSocketSession session) throws InterruptedException {
        Date timestamp = gson.fromJson(request.get(Constants.TIMESTAMP), Date.class);
        Set<String> devices = gson.fromJson(request.get(Constants.DEVICE_GUIDS), JsonTypes.STRING_SET_TYPE);
        Set<String> names = gson.fromJson(request.get(Constants.NAMES), JsonTypes.STRING_SET_TYPE);
        String deviceId = Optional.ofNullable(request.get(Constants.DEVICE_GUID))
                .map(JsonElement::getAsString)
                .orElse(null);

        logger.debug("notification/subscribe requested for devices: {}, {}. Timestamp: {}. Names {} Session: {}",
                devices, deviceId, timestamp, names, session.getId());

        devices = prepareActualList(devices, deviceId);

        List<DeviceVO> actualDevices;
        if (devices != null) {
            actualDevices = deviceService.findByGuidWithPermissionsCheck(devices,
                    (HivePrincipal) ((HiveAuthentication) session.getPrincipal()).getPrincipal());
            if (actualDevices.size() != devices.size()) {
                throw new HiveException(String.format(Messages.DEVICES_NOT_FOUND, devices), SC_FORBIDDEN);
            }
        } else {
            devices = Collections.singleton(Constants.NULL_SUBSTITUTE);
        }

        BiConsumer<DeviceNotification, String> callback = (notification, subscriptionId) -> {
            JsonObject json = ServerResponsesFactory.createNotificationInsertMessage(notification, subscriptionId);
            sendMessage(json, session);
        };

        Pair<String, CompletableFuture<List<DeviceNotification>>> pair = notificationService
                .sendSubscribeRequest(devices, names, timestamp, callback);

        pair.getRight().thenAccept(collection -> collection.forEach(notification -> {
            JsonObject json = ServerResponsesFactory.createNotificationInsertMessage(notification, pair.getLeft());
            sendMessage(json, session);
        }));

        logger.debug("notification/subscribe done for devices: {}, {}. Timestamp: {}. Names {} Session: {}",
                devices, deviceId, timestamp, names, session.getId());

        ((CopyOnWriteArraySet) session
                .getAttributes()
                .get(SUBSCSRIPTION_SET_NAME))
                .add(pair.getLeft());

        WebSocketResponse response = new WebSocketResponse();
        response.addValue(SUBSCRIPTION_ID, pair.getLeft(), null);
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

    /**
     * Implementation of the <a href="http://www.devicehive.com/restful#WsReference/Client/notificationunsubscribe">
     * WebSocket API: Client: notification/unsubscribe</a> Unsubscribes from device notifications.
     *
     * @param session Current session
     * @return Json object with the following structure <code> { "action": {string}, "status": {string}, "requestId":
     * {object} } </code>
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'CLIENT', 'KEY') and hasPermission(null, 'GET_DEVICE_NOTIFICATION')")
    public WebSocketResponse processNotificationUnsubscribe(JsonObject request,
                                                            WebSocketSession session) {
        Optional<String> subId = Optional.ofNullable(request.get(SUBSCRIPTION_ID))
                .map(JsonElement::getAsString);
        Set<String> deviceGuids = gson.fromJson(request.get(DEVICE_GUIDS), JsonTypes.STRING_SET_TYPE);
        logger.debug("notification/unsubscribe action. Session {} ", session.getId());
        if (!subId.isPresent() && deviceGuids == null) {
            Set<String> subForAll = new HashSet<String>() {
                {
                    add(Constants.NULL_SUBSTITUTE);
                }

                private static final long serialVersionUID = 8001668138178383978L;
            };
            notificationService.submitNotificationUnsubscribe(null, subForAll);
        } else if (subId.isPresent()) {
            notificationService.submitNotificationUnsubscribe(subId.get(), deviceGuids);
        } else {
            notificationService.submitNotificationUnsubscribe(null, deviceGuids);
        }
        logger.debug("notification/unsubscribe completed for session {}", session.getId());

        ((CopyOnWriteArraySet) session
                .getAttributes()
                .get(SUBSCSRIPTION_SET_NAME))
                .remove(subId);
        return new WebSocketResponse();
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CLIENT', 'KEY') and hasPermission(null, 'CREATE_DEVICE_NOTIFICATION')")
    public WebSocketResponse processNotificationInsert(JsonObject request,
                                                       WebSocketSession session) {
        final String deviceGuid = Optional.ofNullable(request.get(Constants.DEVICE_GUID))
                .map(JsonElement::getAsString)
                .orElse(null);
        DeviceNotificationWrapper notificationSubmit = gson.fromJson(request.get(Constants.NOTIFICATION), DeviceNotificationWrapper.class);

        logger.debug("notification/insert requested. Session {}. Guid {}", session, deviceGuid);
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (notificationSubmit == null || notificationSubmit.getNotification() == null) {
            logger.debug(
                    "notification/insert proceed with error. Bad notification: notification is required.");
            throw new HiveException(Messages.NOTIFICATION_REQUIRED, SC_BAD_REQUEST);
        }

        DeviceVO device;
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

        WebSocketResponse response = new WebSocketResponse();
        notificationService.submitDeviceNotification(message, device)
                .thenApply(notification -> {
                    logger.debug("notification/insert proceed successfully. Session {}. Guid {}", session, deviceGuid);
                    response.addValue(NOTIFICATION, new InsertNotification(message.getId(), message.getTimestamp()), NOTIFICATION_TO_DEVICE);
                    return response;
                })
                .exceptionally(ex -> {
                    logger.warn("Unable to insert notification.", ex);
                    throw new HiveException(Messages.INTERNAL_SERVER_ERROR, SC_INTERNAL_SERVER_ERROR);
                }).join();
        return response;
    }
}
