package com.devicehive.resource.impl;

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
import com.devicehive.configuration.Messages;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.SpecialNotifications;
import com.devicehive.model.eventbus.Filter;
import com.devicehive.model.websockets.InsertNotification;
import com.devicehive.model.wrappers.DeviceNotificationWrapper;
import com.devicehive.resource.DeviceNotificationResource;
import com.devicehive.model.converters.TimestampQueryParamParser;
import com.devicehive.resource.util.CommandResponseFilterAndSort;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.DeviceNotificationService;
import com.devicehive.service.DeviceService;
import com.devicehive.service.NetworkService;
import com.devicehive.service.time.TimestampService;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.NetworkWithUsersAndDevicesVO;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static javax.ws.rs.core.Response.Status.*;

/**
 * {@inheritDoc}
 */
@Service
public class DeviceNotificationResourceImpl implements DeviceNotificationResource {
    private static final Logger logger = LoggerFactory.getLogger(DeviceNotificationResourceImpl.class);

    @Autowired
    private Gson gson;

    @Autowired
    private DeviceNotificationService notificationService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private NetworkService networkService;

    @Autowired
    private TimestampService timestampService;

    @Autowired
    private HiveValidator hiveValidator;

    /**
     * {@inheritDoc}
     */
    @Override
    public void query(String deviceId, String startTs, String endTs, String notification, String sortField,
                      String sortOrderSt, Integer take, Integer skip, @Suspended final AsyncResponse asyncResponse) {
        logger.debug("Device notification query requested for device {}", deviceId);

        final Date timestampSt = TimestampQueryParamParser.parse(startTs);
        final Date timestampEnd = TimestampQueryParamParser.parse(endTs);

        DeviceVO byIdWithPermissionsCheck = deviceService.findById(deviceId);
        if (byIdWithPermissionsCheck == null) {
            ErrorResponse errorCode = new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(Messages.DEVICE_NOT_FOUND, deviceId));
            Response response = ResponseFactory.response(NOT_FOUND, errorCode);
            asyncResponse.resume(response);
        } else {
            Set<String> notificationNames = StringUtils.isNoneEmpty(notification)
                    ? Collections.singleton(notification)
                    : Collections.emptySet();
            notificationService.find(Collections.singleton(deviceId), notificationNames, timestampSt, timestampEnd)
                    .thenApply(notifications -> {
                        final Comparator<DeviceNotification> comparator = CommandResponseFilterAndSort
                                .buildDeviceNotificationComparator(sortField);
                        final Boolean reverse = sortOrderSt == null ? null : "desc".equalsIgnoreCase(sortOrderSt);

                        final List<DeviceNotification> sortedDeviceNotifications = CommandResponseFilterAndSort
                                .orderAndLimit(notifications, comparator, reverse, skip, take);
                        return ResponseFactory.response(OK, sortedDeviceNotifications, JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT);
                    })
                    .thenAccept(asyncResponse::resume);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get(String deviceId, Long notificationId, @Suspended final AsyncResponse asyncResponse) {
        logger.debug("Device notification requested. deviceId {}, notification id {}", deviceId, notificationId);

        DeviceVO device = deviceService.findById(deviceId);

        if (device == null) {
            ErrorResponse errorCode = new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(Messages.DEVICE_NOT_FOUND, deviceId));
            Response response = ResponseFactory.response(NOT_FOUND, errorCode);
            asyncResponse.resume(response);
        } else {
            notificationService.findOne(notificationId, deviceId)
                    .thenApply(notification -> notification
                            .map(n -> {
                                logger.debug("Device notification proceed successfully");
                                return ResponseFactory.response(Response.Status.OK, n, JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT);
                            }).orElseGet(() -> {
                                logger.error(String.format(Messages.NOTIFICATION_NOT_FOUND_LOG, notificationId, deviceId));
                                ErrorResponse errorCode = new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(Messages.NOTIFICATION_NOT_FOUND, notificationId));
                                return ResponseFactory.response(NOT_FOUND, errorCode);
                            }))
                    .exceptionally(e -> {
                        //TODO: change error message here
                        logger.warn("Device notification get failed. NOT FOUND: No notification with id = {} found for device with deviceId = {}", notificationId, deviceId);
                        ErrorResponse errorCode = new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(Messages.NOTIFICATION_NOT_FOUND, notificationId));
                        return ResponseFactory.response(NOT_FOUND, errorCode);
                    })
                    .thenAccept(asyncResponse::resume);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void poll(final String deviceId, final String namesString, final String timestamp,
                     final long timeout, final AsyncResponse asyncResponse) throws Exception {
        poll(timeout, deviceId, null, namesString, timestamp, asyncResponse);
    }

    @Override
    public void pollMany(final long timeout, String deviceIdsString, String networkIdsString, final String namesString,
                         final String timestamp, final AsyncResponse asyncResponse) throws Exception {
        poll(timeout, deviceIdsString, networkIdsString, namesString, timestamp, asyncResponse);
    }

    private void poll(final long timeout,
                      final String deviceIdsString,
                      final String networkIdsCsv,
                      final String namesString,
                      final String timestamp,
                      final AsyncResponse asyncResponse) throws InterruptedException {
        final HiveAuthentication authentication = (HiveAuthentication) SecurityContextHolder.getContext().getAuthentication();
        final HivePrincipal principal = (HivePrincipal) authentication.getPrincipal();
        final Date ts = Optional.ofNullable(timestamp)
                .map(TimestampQueryParamParser::parse)
                .orElse(timestampService.getDate());
        
        final Response response = ResponseFactory.response(
                Response.Status.OK,
                Collections.emptyList(),
                JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT);

        asyncResponse.setTimeoutHandler(asyncRes -> asyncRes.resume(response));

        Set<String> availableDevices = new HashSet<>();
        if (deviceIdsString != null) {
            availableDevices = Optional.ofNullable(StringUtils.split(deviceIdsString, ','))
                    .map(Arrays::asList)
                    .map(list -> deviceService.findByIdWithPermissionsCheck(list, principal))
                    .map(list -> list.stream().map(DeviceVO::getDeviceId).collect(Collectors.toSet()))
                    .orElse(Collections.emptySet());
        }
        if (networkIdsCsv != null) {
            Set<String> networkDevices = Optional.ofNullable(StringUtils.split(networkIdsCsv, ','))
                    .map(Arrays::asList)
                    .map(list -> list.stream()
                            .map(n -> gson.fromJson(n, Long.class))
                            .map(network -> networkService.getWithDevices(network, authentication))
                            .filter(Objects::nonNull).map(NetworkWithUsersAndDevicesVO::getDevices)
                            .flatMap(Collection::stream)
                            .map(DeviceVO::getDeviceId)
                            .collect(Collectors.toSet())
                    ).orElse(Collections.emptySet());
            availableDevices.addAll(networkDevices);
        }
        if (availableDevices.isEmpty()) {
            availableDevices = deviceService.findByIdWithPermissionsCheck(Collections.emptyList(), principal)
                    .stream()
                    .map(DeviceVO::getDeviceId)
                    .collect(Collectors.toSet());

        }

        Set<String> notifications = Optional.ofNullable(StringUtils.split(namesString, ','))
                .map(Arrays::asList)
                .map(list -> list.stream().collect(Collectors.toSet()))
                .orElse(Collections.emptySet());

        BiConsumer<DeviceNotification, Long> callback = (notification, subscriptionId) -> {
            if (!asyncResponse.isDone()) {
                asyncResponse.resume(ResponseFactory.response(
                        Response.Status.OK,
                        Collections.singleton(notification),
                        JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT));
            }
        };

        Filter filter = new Filter();
        filter.setNames(notifications);
        if (!availableDevices.isEmpty()) {
            Pair<Long, CompletableFuture<List<DeviceNotification>>> pair = notificationService
                    .subscribe(availableDevices, filter, ts, callback);
            pair.getRight().thenAccept(collection -> {
                if (!collection.isEmpty() && !asyncResponse.isDone()) {
                    asyncResponse.resume(ResponseFactory.response(
                            Response.Status.OK,
                            collection,
                            JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT));
                }

                if (timeout == 0) {
                    asyncResponse.setTimeout(1, TimeUnit.MILLISECONDS); // setting timeout to 0 would cause
                    // the thread to suspend indefinitely, see AsyncResponse docs
                } else {
                    asyncResponse.setTimeout(timeout, TimeUnit.SECONDS);
                }
            });

            asyncResponse.register(new CompletionCallback() {
                @Override
                public void onComplete(Throwable throwable) {
                    notificationService.unsubscribe(pair.getLeft(), null);
                }
            });
        } else {
            if (!asyncResponse.isDone()) {
                asyncResponse.resume(response);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert(String deviceId, DeviceNotificationWrapper notificationSubmit, @Suspended final AsyncResponse asyncResponse) {
        hiveValidator.validate(notificationSubmit);
        logger.debug("DeviceNotification insert requested: {}", notificationSubmit);
        final String notificationName = notificationSubmit.getNotification();
        if (notificationName == null) {
            logger.warn("DeviceNotification insert proceed with error. BAD REQUEST: notification is required.");
            ErrorResponse errorResponseEntity = new ErrorResponse(BAD_REQUEST.getStatusCode(),
                    Messages.INVALID_REQUEST_PARAMETERS);
            Response response = ResponseFactory.response(BAD_REQUEST, errorResponseEntity);
            asyncResponse.resume(response);
        } else if (SpecialNotifications.DEVICE_UPDATE.equals(notificationName) || // Prevent inserting special notification manually
                    SpecialNotifications.DEVICE_ADD.equals(notificationName)){
            logger.warn("DeviceNotification insert proceed with error. FORBIDDEN: it's not allow to insert special notification.");
            ErrorResponse errorCode = new ErrorResponse(FORBIDDEN.getStatusCode(), Messages.FORBIDDEN_INSERT_SPECIAL_NOTIFICATION);
            Response response = ResponseFactory.response(FORBIDDEN, errorCode);
            asyncResponse.resume(response);
        } else {
            DeviceVO device = deviceService.findById(deviceId);
            if (device == null) {
                logger.warn("DeviceNotification insert proceed with error. NOT FOUND: device {} not found.", deviceId);
                Response response = ResponseFactory.response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(),
                        String.format(Messages.DEVICE_NOT_FOUND, deviceId)));
                asyncResponse.resume(response);
            } else {
                if (device.getNetworkId() == null) {
                    logger.warn("DeviceNotification insert proceed with error. FORBIDDEN: Device {} is not connected to network.", deviceId);
                    Response response = ResponseFactory.response(FORBIDDEN, new ErrorResponse(FORBIDDEN.getStatusCode(),
                            String.format(Messages.DEVICE_IS_NOT_CONNECTED_TO_NETWORK, deviceId)));
                    asyncResponse.resume(response);
                } else {
                    DeviceNotification toInsert = notificationService.convertWrapperToNotification(notificationSubmit, device);
                    notificationService.insert(toInsert, device)
                            .thenAccept(notification -> {
                                logger.debug("Device notification insert proceed successfully. deviceId = {} notification = {}",
                                        deviceId, notification.getNotification());

                                asyncResponse.resume(ResponseFactory.response(
                                        Response.Status.CREATED,
                                        new InsertNotification(notification.getId(), notification.getTimestamp()),
                                        JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT));
                            })
                            .exceptionally(e -> {
                                logger.warn("Device notification insert failed for device with deviceId = {}.", deviceId);
                                ErrorResponse errorCode = new ErrorResponse(INTERNAL_SERVER_ERROR.getStatusCode(), String.format(Messages.NOTIFICATION_INSERT_FAILED, deviceId));
                                Response jaxResponse = ResponseFactory.response(INTERNAL_SERVER_ERROR, errorCode);
                                asyncResponse.resume(jaxResponse);
                                return null;
                            });
                }
            }
        }
    }
}
