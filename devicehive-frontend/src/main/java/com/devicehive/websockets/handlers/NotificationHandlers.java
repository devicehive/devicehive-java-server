package com.devicehive.websockets.handlers;

/*
 * #%L
 * DeviceHive Frontend Logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.messages.handler.WebSocketClientHandler;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.rpc.ListDeviceRequest;
import com.devicehive.model.rpc.ListNotificationRequest;
import com.devicehive.model.websockets.InsertNotification;
import com.devicehive.model.wrappers.DeviceNotificationWrapper;
import com.devicehive.resource.util.CommandResponseFilterAndSort;
import com.devicehive.resource.util.JsonTypes;
import com.devicehive.service.DeviceNotificationService;
import com.devicehive.service.DeviceService;
import com.devicehive.util.ServerResponsesFactory;
import com.devicehive.vo.DeviceVO;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static com.devicehive.configuration.Constants.*;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_TO_DEVICE;
import static com.devicehive.model.enums.SortOrder.ASC;
import static com.devicehive.model.rpc.ListNotificationRequest.createListNotificationRequest;
import static javax.servlet.http.HttpServletResponse.*;

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

    @Autowired
    private WebSocketClientHandler clientHandler;


    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_DEVICE_NOTIFICATION')")
    public void processNotificationSubscribe(JsonObject request,
                                                          WebSocketSession session) throws InterruptedException, IOException {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Date timestamp = gson.fromJson(request.get(Constants.TIMESTAMP), Date.class);
        Set<String> devices = gson.fromJson(request.get(Constants.DEVICE_IDS), JsonTypes.STRING_SET_TYPE);
        Set<String> names = gson.fromJson(request.get(Constants.NAMES), JsonTypes.STRING_SET_TYPE);
        String deviceId = gson.fromJson(request.get(DEVICE_ID), String.class);

        logger.debug("notification/subscribe requested for devices: {}, {}. Timestamp: {}. Names {} Session: {}",
                devices, deviceId, timestamp, names, session.getId());

        devices = prepareActualList(devices, deviceId);

        List<DeviceVO> actualDevices;
        if (devices != null) {
            actualDevices = deviceService.findByIdWithPermissionsCheck(devices, principal);
            if (actualDevices.size() != devices.size()) {
                throw new HiveException(String.format(Messages.DEVICES_NOT_FOUND, devices), SC_FORBIDDEN);
            }
        } else {
            ListDeviceRequest listDeviceRequest = new ListDeviceRequest(ASC.name(), principal);
            actualDevices = deviceService.list(listDeviceRequest).join();
            devices = actualDevices.stream().map(DeviceVO::getDeviceId).collect(Collectors.toSet());
        }

        BiConsumer<DeviceNotification, String> callback = (notification, subscriptionId) -> {
            JsonObject json = ServerResponsesFactory.createNotificationInsertMessage(notification, subscriptionId);
            clientHandler.sendMessage(json, session);
        };

        Pair<String, CompletableFuture<List<DeviceNotification>>> pair = notificationService
                .subscribe(devices, names, timestamp, callback);

        pair.getRight().thenAccept(collection -> collection.forEach(notification -> {
            JsonObject json = ServerResponsesFactory.createNotificationInsertMessage(notification, pair.getLeft());
            clientHandler.sendMessage(json, session);
        }));

        logger.debug("notification/subscribe done for devices: {}, {}. Timestamp: {}. Names {} Session: {}",
                devices, deviceId, timestamp, names, session.getId());

        ((CopyOnWriteArraySet) session
                .getAttributes()
                .get(SUBSCSRIPTION_SET_NAME))
                .add(pair.getLeft());

        WebSocketResponse response = new WebSocketResponse();
        response.addValue(SUBSCRIPTION_ID, pair.getLeft(), null);
        clientHandler.sendMessage(request, response, session);
    }

    /**
     * Implementation of the <a href="http://www.devicehive.com/restful#WsReference/Client/notificationunsubscribe">
     * WebSocket API: Client: notification/unsubscribe</a> Unsubscribes from device notifications.
     *
     * @param session Current session
     * @return Json object with the following structure <code> { "action": {string}, "status": {string}, "requestId":
     * {object} } </code>
     */
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_DEVICE_NOTIFICATION')")
    public void processNotificationUnsubscribe(JsonObject request,
                                                            WebSocketSession session) throws IOException {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final String subscriptionId = gson.fromJson(request.get(SUBSCRIPTION_ID), String.class);
        Set<String> deviceIds = gson.fromJson(request.get(DEVICE_IDS), JsonTypes.STRING_SET_TYPE);
        CopyOnWriteArraySet sessionSubIds = ((CopyOnWriteArraySet) session
                .getAttributes()
                .get(SUBSCSRIPTION_SET_NAME));

        logger.debug("notification/unsubscribe action. Session {} ", session.getId());
        if (subscriptionId != null && !sessionSubIds.contains(subscriptionId)) {
            throw new HiveException(String.format(Messages.SUBSCRIPTION_NOT_FOUND, subscriptionId), SC_NOT_FOUND);
        }
        if (subscriptionId == null && deviceIds == null) {
            ListDeviceRequest listDeviceRequest = new ListDeviceRequest(ASC.name(), principal);
            List<DeviceVO> actualDevices = deviceService.list(listDeviceRequest).join();
            deviceIds = actualDevices.stream().map(DeviceVO::getDeviceId).collect(Collectors.toSet());
            notificationService.unsubscribe(null, deviceIds);
        } else {
            notificationService.unsubscribe(subscriptionId, deviceIds);
        }
        logger.debug("notification/unsubscribe completed for session {}", session.getId());

        sessionSubIds.remove(subscriptionId);
        clientHandler.sendMessage(request, new WebSocketResponse(), session);
    }

    @PreAuthorize("isAuthenticated() and hasPermission(null, 'CREATE_DEVICE_NOTIFICATION')")
    public void processNotificationInsert(JsonObject request,
                                                       WebSocketSession session) {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final String deviceId = gson.fromJson(request.get(DEVICE_ID), String.class);
        DeviceNotificationWrapper notificationSubmit = gson.fromJson(request.get(Constants.NOTIFICATION), DeviceNotificationWrapper.class);

        logger.debug("notification/insert requested. Session {}. Device ID {}", session, deviceId);
        if (notificationSubmit == null || notificationSubmit.getNotification() == null) {
            logger.error("notification/insert proceed with error. Bad notification: notification is required.");
            throw new HiveException(Messages.NOTIFICATION_REQUIRED, SC_BAD_REQUEST);
        }

        if (deviceId == null) {
            logger.error("notification/insert proceed with error. Device ID should be provided");
            throw new HiveException(Messages.DEVICE_ID_REQUIRED, SC_BAD_REQUEST);
        }

        final DeviceVO device = deviceService.findByIdWithPermissionsCheck(deviceId, principal);

        if (device == null) {
            logger.error("notification/insert proceed with error. No device with Device ID = {} found.", deviceId);
            throw new HiveException(String.format(Messages.DEVICE_NOT_FOUND, deviceId), SC_NOT_FOUND);
        }

        WebSocketResponse response = new WebSocketResponse();

        if (device.getNetworkId() == null) {
            logger.error("notification/insert. No network specified for device with Device ID = {}", deviceId);
            throw new HiveException(String.format(Messages.DEVICE_IS_NOT_CONNECTED_TO_NETWORK, deviceId), SC_FORBIDDEN);
        }
        DeviceNotification message = notificationService.convertWrapperToNotification(notificationSubmit, device);

        notificationService.insert(message, device)
                .thenAccept(notification -> {
                    logger.debug("notification/insert proceed successfully. Session {}. Device ID {}", session, deviceId);
                    response.addValue(NOTIFICATION, new InsertNotification(message.getId(), message.getTimestamp()), NOTIFICATION_TO_DEVICE);
                    clientHandler.sendMessage(request, response, session);
                });
    }

    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_DEVICE_NOTIFICATION')")
    public void processNotificationGet(JsonObject request, WebSocketSession session) {
        String deviceId = gson.fromJson(request.get(DEVICE_ID), String.class);
        if (deviceId == null) {
            logger.error("notification/get proceed with error. Device ID should be provided.");
            throw new HiveException(Messages.DEVICE_ID_REQUIRED, SC_BAD_REQUEST);
        }
                
        Long notificationId = gson.fromJson(request.get(NOTIFICATION_ID), Long.class);
        if (notificationId == null) {
            logger.error("notification/get proceed with error. Notification ID should be provided.");
            throw new HiveException(Messages.NOTIFICATION_ID_REQUIRED, SC_BAD_REQUEST);
        }
        
        logger.debug("Device notification requested. deviceId {}, notification id {}", deviceId, notificationId);

        DeviceVO device = deviceService.findById(deviceId);

        if (device == null) {
            logger.error("notification/get proceed with error. No Device with Device ID = {} found.", deviceId);
            throw new HiveException(String.format(Messages.DEVICE_NOT_FOUND, deviceId), SC_NOT_FOUND);
        }

        notificationService.findOne(notificationId, deviceId)
                .thenAccept(notification -> {
                    logger.debug("Device notification proceed successfully");
                    WebSocketResponse response = new WebSocketResponse();
                    if (!notification.isPresent()) {
                        logger.error("Notification with id {} not found", notificationId);
                        clientHandler.sendErrorResponse(request, SC_NOT_FOUND, Messages.NOTIFICATION_NOT_FOUND, session);
                    } else {
                        response.addValue(NOTIFICATION, notification.get(), NOTIFICATION_TO_CLIENT);
                        clientHandler.sendMessage(request, response, session);
                    }
                });
    }

    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_DEVICE_NOTIFICATION')")
    public void processNotificationList(JsonObject request, WebSocketSession session) {
        ListNotificationRequest listNotificationRequest = createListNotificationRequest(request);
        String deviceId = listNotificationRequest.getDeviceId();
        if (deviceId == null) {
            logger.error("notification/list proceed with error. Device ID should be provided.");
            throw new HiveException(Messages.DEVICE_ID_REQUIRED, SC_BAD_REQUEST);
        }
        
        logger.debug("Device notification query requested for device {}", deviceId);

        DeviceVO byIdWithPermissionsCheck = deviceService.findById(deviceId);
        if (byIdWithPermissionsCheck == null) {
            logger.error("notification/get proceed with error. No Device with Device ID = {} found.", deviceId);
            throw new HiveException(String.format(Messages.DEVICE_NOT_FOUND, deviceId), SC_NOT_FOUND);
        }
        
        WebSocketResponse response = new WebSocketResponse();
        
        notificationService.find(listNotificationRequest)
                .thenAccept(notifications -> {
                    final Comparator<DeviceNotification> comparator = CommandResponseFilterAndSort
                            .buildDeviceNotificationComparator(listNotificationRequest.getSortField());
                    String sortOrderSt = listNotificationRequest.getSortOrder();
                    final Boolean reverse = sortOrderSt == null ? null : "desc".equalsIgnoreCase(sortOrderSt);

                    final List<DeviceNotification> sortedDeviceNotifications = CommandResponseFilterAndSort
                            .orderAndLimit(notifications, comparator, reverse,
                                    listNotificationRequest.getSkip(), listNotificationRequest.getTake());
                    response.addValue(NOTIFICATIONS, sortedDeviceNotifications, NOTIFICATION_TO_CLIENT);
                    clientHandler.sendMessage(request, response, session);
                });
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
}
