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

import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.websockets.HiveWebsocketAuth;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.exceptions.IllegalParametersException;
import com.devicehive.messages.handler.WebSocketClientHandler;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.eventbus.Filter;
import com.devicehive.model.rpc.ListDeviceRequest;
import com.devicehive.model.rpc.ListNotificationRequest;
import com.devicehive.model.websockets.InsertNotification;
import com.devicehive.model.wrappers.DeviceNotificationWrapper;
import com.devicehive.resource.util.CommandResponseFilterAndSort;
import com.devicehive.resource.util.JsonTypes;
import com.devicehive.service.DeviceNotificationService;
import com.devicehive.service.DeviceService;
import com.devicehive.service.NetworkService;
import com.devicehive.shim.api.Action;
import com.devicehive.util.ServerResponsesFactory;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.NetworkWithUsersAndDevicesVO;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
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

    private final DeviceService deviceService;
    private final NetworkService networkService;
    private final DeviceNotificationService notificationService;
    private final Gson gson;
    private final WebSocketClientHandler clientHandler;

    @Autowired
    public NotificationHandlers(DeviceService deviceService,
                                NetworkService networkService,
                                DeviceNotificationService notificationService,
                                Gson gson,
                                WebSocketClientHandler clientHandler) {
        this.deviceService = deviceService;
        this.networkService = networkService;
        this.notificationService = notificationService;
        this.gson = gson;
        this.clientHandler = clientHandler;
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(#deviceId, 'GET_DEVICE_NOTIFICATION')")
    public void processNotificationSubscribe(String deviceId, JsonObject request,
                                                          WebSocketSession session) throws InterruptedException, IOException {
        final HiveAuthentication authentication = (HiveAuthentication) SecurityContextHolder.getContext().getAuthentication();
        final HivePrincipal principal = (HivePrincipal) authentication.getPrincipal();
        final Date timestamp = gson.fromJson(request.get(Constants.TIMESTAMP), Date.class);
        Set<String> devices = gson.fromJson(request.get(Constants.DEVICE_IDS), JsonTypes.STRING_SET_TYPE);
        final Set<Long> networks = gson.fromJson(request.getAsJsonArray(NETWORK_IDS), JsonTypes.LONG_SET_TYPE);
        final Set<String> names = gson.fromJson(request.get(Constants.NAMES), JsonTypes.STRING_SET_TYPE);
        
        logger.debug("notification/subscribe requested for devices: {}, {}. Networks: {}. Timestamp: {}. Names {} Session: {}",
                devices, deviceId, networks, timestamp, names, session.getId());

        devices = prepareActualList(devices, deviceId);

        final Filter filter = new Filter();
        filter.setNames(names);
        filter.setPrincipal(principal);
        filter.setEventName(Action.NOTIFICATION_EVENT.name());
        if (!devices.isEmpty()) {
            deviceService.getAllowedExistingDevices(devices, principal);
            filter.setDeviceIds(devices);
        }
        if (networks != null) {
            final Set<NetworkWithUsersAndDevicesVO> actualNetworks = networks.stream().map(network ->
                    networkService.getWithDevices(network, authentication)
            ).filter(Objects::nonNull).collect(Collectors.toSet());
            if (actualNetworks.size() != networks.size()) {
                throw new HiveException(String.format(Messages.NETWORKS_NOT_FOUND, networks), SC_FORBIDDEN);
            }
            final Set<String> networkDevices = actualNetworks.stream()
                    .map(NetworkWithUsersAndDevicesVO::getDevices)
                    .flatMap(Collection::stream)
                    .map(DeviceVO::getDeviceId)
                    .collect(Collectors.toSet());
            devices.addAll(networkDevices);
            filter.setNetworkIds(networks);
        }
        if (devices.isEmpty()) {
            final ListDeviceRequest listDeviceRequest = new ListDeviceRequest(ASC.name(), principal);
            final List<DeviceVO> actualDevices = deviceService.list(listDeviceRequest);

            devices = actualDevices.stream().map(DeviceVO::getDeviceId).collect(Collectors.toSet());
            filter.setGlobal(true);
        }

        final BiConsumer<DeviceNotification, Long> callback = (notification, subscriptionId) -> {
            final JsonObject json = ServerResponsesFactory.createNotificationInsertMessage(notification, subscriptionId);
            clientHandler.sendMessage(json, session);
        };

        final Pair<Long, CompletableFuture<List<DeviceNotification>>> pair = notificationService
                .subscribe(devices, filter, timestamp, callback);

        logger.debug("notification/subscribe done for devices: {}, {}. Networks: {}. Timestamp: {}. Names {} Session: {}",
                devices, deviceId, networks, timestamp, names, session.getId());

        ((CopyOnWriteArraySet) session
                .getAttributes()
                .get(SUBSCSRIPTION_SET_NAME))
                .add(pair.getLeft());

        pair.getRight().thenAccept(collection -> {
            final WebSocketResponse response = new WebSocketResponse();
            response.addValue(SUBSCRIPTION_ID, pair.getLeft(), null);
            clientHandler.sendMessage(request, response, session);
            collection.forEach(notification -> {
                final JsonObject json = ServerResponsesFactory.createNotificationInsertMessage(notification, pair.getLeft());
                clientHandler.sendMessage(json, session);
            });
        });
    }

    /**
     * Implementation of the <a href="http://www.devicehive.com/restful#WsReference/Client/notificationunsubscribe">
     * WebSocket API: Client: notification/unsubscribe</a> Unsubscribes from device notifications.
     *
     * @param session Current session
     * @return Json object with the following structure <code> { "action": {string}, "status": {string}, "requestId":
     * {object} } </code>
     */
    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_DEVICE_NOTIFICATION')")
    public void processNotificationUnsubscribe(JsonObject request,
                                                            WebSocketSession session) throws IOException {
        final HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final Long subscriptionId = gson.fromJson(request.get(SUBSCRIPTION_ID), Long.class);
        final CopyOnWriteArraySet<Long> sessionSubIds = ((CopyOnWriteArraySet) session
                .getAttributes()
                .get(SUBSCSRIPTION_SET_NAME));

        logger.debug("notification/unsubscribe action. Session {} ", session.getId());
        if (subscriptionId != null && !sessionSubIds.contains(subscriptionId)) {
            throw new HiveException(String.format(Messages.SUBSCRIPTION_NOT_FOUND, subscriptionId), SC_NOT_FOUND);
        }
        CompletableFuture<Set<Long>> future;
        if (subscriptionId == null) {
            future = notificationService.unsubscribe(sessionSubIds);
            sessionSubIds.clear();
        } else {
            future = notificationService.unsubscribe(Collections.singleton(subscriptionId));
            sessionSubIds.remove(subscriptionId);
        }
        
        future.thenAccept(collection -> {
            logger.debug("notification/unsubscribe completed for session {}", session.getId());
            clientHandler.sendMessage(request, new WebSocketResponse(), session);
        });
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(#deviceId, 'CREATE_DEVICE_NOTIFICATION')")
    public void processNotificationInsert(String deviceId, JsonObject request,
                                                       WebSocketSession session) {
        final HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final DeviceNotificationWrapper notificationSubmit = gson.fromJson(request.get(Constants.NOTIFICATION), DeviceNotificationWrapper.class);

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

        final WebSocketResponse response = new WebSocketResponse();

        if (device.getNetworkId() == null) {
            logger.error("notification/insert. No network specified for device with Device ID = {}", deviceId);
            throw new HiveException(String.format(Messages.DEVICE_IS_NOT_CONNECTED_TO_NETWORK, deviceId), SC_FORBIDDEN);
        }
        final DeviceNotification message = notificationService.convertWrapperToNotification(notificationSubmit, device);

        notificationService.insert(message, device)
                .thenAccept(notification -> {
                    logger.debug("notification/insert proceed successfully. Session {}. Device ID {}", session, deviceId);
                    response.addValue(NOTIFICATION, new InsertNotification(notification.getId(), notification.getTimestamp()), NOTIFICATION_TO_DEVICE);
                    clientHandler.sendMessage(request, response, session);
                });
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_DEVICE_NOTIFICATION')")
    public void processNotificationGet(JsonObject request, WebSocketSession session) {
        try {
            final String deviceId = gson.fromJson(request.get(DEVICE_ID), String.class);
            if (deviceId == null) {
                logger.error("notification/get proceed with error. Device ID should be provided.");
                throw new HiveException(Messages.DEVICE_ID_REQUIRED, SC_BAD_REQUEST);
            }

            final Long notificationId = gson.fromJson(request.get(NOTIFICATION_ID), Long.class);

            if (notificationId == null) {
                logger.error("notification/get proceed with error. Notification ID should be provided.");
                throw new HiveException(Messages.NOTIFICATION_ID_REQUIRED, SC_BAD_REQUEST);
            }

            logger.debug("Device notification requested. deviceId {}, notification id {}", deviceId, notificationId);

            final DeviceVO device = deviceService.findById(deviceId);

            if (device == null) {
                logger.error("notification/get proceed with error. No Device with Device ID = {} found.", deviceId);
                throw new HiveException(String.format(Messages.DEVICE_NOT_FOUND, deviceId), SC_NOT_FOUND);
            }

            notificationService.findOne(notificationId, deviceId)
                    .thenAccept(notification -> {
                        logger.debug("Device notification proceed successfully");
                        final WebSocketResponse response = new WebSocketResponse();
                        if (!notification.isPresent()) {
                            logger.error("Notification with id {} not found", notificationId);
                            clientHandler.sendErrorResponse(request, SC_NOT_FOUND, Messages.NOTIFICATION_NOT_FOUND, session);
                        } else {
                            response.addValue(NOTIFICATION, notification.get(), NOTIFICATION_TO_CLIENT);
                            clientHandler.sendMessage(request, response, session);
                        }
                    });
        } catch (JsonParseException ex) {
            final String errorMessage = "Notification id should be an integer value.";
            logger.error("notification/get proceed with error: {}", errorMessage);
            throw new IllegalParametersException(errorMessage);
        }
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_DEVICE_NOTIFICATION')")
    public void processNotificationList(JsonObject request, WebSocketSession session) {
        final ListNotificationRequest listNotificationRequest = createListNotificationRequest(request);
        final String deviceId = listNotificationRequest.getDeviceId();
        if (deviceId == null) {
            logger.error("notification/list proceed with error. Device ID should be provided.");
            throw new HiveException(Messages.DEVICE_ID_REQUIRED, SC_BAD_REQUEST);
        }
        
        logger.debug("Device notification query requested for device {}", deviceId);

        final DeviceVO byIdWithPermissionsCheck = deviceService.findById(deviceId);
        if (byIdWithPermissionsCheck == null) {
            logger.error("notification/get proceed with error. No Device with Device ID = {} found.", deviceId);
            throw new HiveException(String.format(Messages.DEVICE_NOT_FOUND, deviceId), SC_NOT_FOUND);
        }

        final WebSocketResponse response = new WebSocketResponse();
        
        notificationService.find(listNotificationRequest)
                .thenAccept(notifications -> {
                    final Comparator<DeviceNotification> comparator = CommandResponseFilterAndSort
                            .buildDeviceNotificationComparator(listNotificationRequest.getSortField());
                    final String sortOrderSt = listNotificationRequest.getSortOrder();
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
            return new HashSet<>();
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
