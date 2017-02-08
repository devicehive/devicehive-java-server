package com.devicehive.service;

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

import com.devicehive.dao.DeviceDao;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.SpecialNotifications;
import com.devicehive.model.eventbus.events.NotificationEvent;
import com.devicehive.model.rpc.*;
import com.devicehive.model.wrappers.DeviceNotificationWrapper;
import com.devicehive.service.helpers.ResponseConsumer;
import com.devicehive.service.time.TimestampService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.util.ServerResponsesFactory;
import com.devicehive.vo.DeviceVO;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class DeviceNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceNotificationService.class);

    private TimestampService timestampService;
    private DeviceDao deviceDao;
    private RpcClient rpcClient;

    @Autowired
    public DeviceNotificationService(TimestampService timestampService,
                                     DeviceDao deviceDao,
                                     RpcClient rpcClient) {
        this.timestampService = timestampService;
        this.deviceDao = deviceDao;
        this.rpcClient = rpcClient;
    }

    public CompletableFuture<Optional<DeviceNotification>> findOne(Long id, String guid) {
        NotificationSearchRequest searchRequest = new NotificationSearchRequest();
        searchRequest.setId(id);
        searchRequest.setGuid(guid);

        CompletableFuture<Response> future = new CompletableFuture<>();
        rpcClient.call(Request.newBuilder()
                .withBody(searchRequest)
                .withPartitionKey(searchRequest.getGuid())
                .build(), new ResponseConsumer(future));
        return future.thenApply(r -> ((NotificationSearchResponse) r.getBody()).getNotifications().stream().findFirst());
    }

    @SuppressWarnings("unchecked")
    public CompletableFuture<List<DeviceNotification>> find(Set<String> guids, Set<String> names,
                                                            Date timestampSt, Date timestampEnd) {
        List<CompletableFuture<Response>> futures = guids.stream()
                .map(guid -> {
                    NotificationSearchRequest searchRequest = new NotificationSearchRequest();
                    searchRequest.setGuid(guid);
                    searchRequest.setNames(names);
                    searchRequest.setTimestampStart(timestampSt);
                    searchRequest.setTimestampEnd(timestampEnd);
                    return searchRequest;
                })
                .map(searchRequest -> {
                    CompletableFuture<Response> future = new CompletableFuture<>();
                    rpcClient.call(Request.newBuilder()
                            .withBody(searchRequest)
                            .withPartitionKey(searchRequest.getGuid())
                            .build(), new ResponseConsumer(future));
                    return future;
                })
                .collect(Collectors.toList());

        // List<CompletableFuture<Response>> => CompletableFuture<List<DeviceNotification>>
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)                                                    // List<CompletableFuture<Response>> => CompletableFuture<List<Response>>
                        .map(r -> r.getBody().cast(NotificationSearchResponse.class).getNotifications()) // CompletableFuture<List<Response>> => CompletableFuture<List<List<DeviceNotification>>>
                        .flatMap(Collection::stream)                                                     // CompletableFuture<List<List<DeviceNotification>>> => CompletableFuture<List<DeviceNotification>>
                        .collect(Collectors.toList()));
    }

    public CompletableFuture<DeviceNotification> insert(final DeviceNotification notification,
                                                        final DeviceVO device) {
        List<CompletableFuture<Response>> futures = processDeviceNotification(notification, device).stream()
                .map(n -> {
                    CompletableFuture<Response> future = new CompletableFuture<>();
                    rpcClient.call(Request.newBuilder()
                            .withBody(new NotificationInsertRequest(n))
                            .withPartitionKey(device.getGuid())
                            .build(), new ResponseConsumer(future));
                    return future;
                })
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                .thenApply(x -> futures.stream()
                        .map(CompletableFuture::join)
                        .map(r -> r.getBody().cast(NotificationInsertResponse.class).getDeviceNotification())
                        .filter(n -> !SpecialNotifications.DEVICE_UPDATE.equals(n.getNotification())) // we are not going to return DEVICE_UPDATE notification
                        .collect(Collectors.toList()).get(0)); // after filter we should get only one notification
    }

    public Pair<String, CompletableFuture<List<DeviceNotification>>> subscribe(
            final Set<String> devices,
            final Set<String> names,
            final Date timestamp,
            final BiConsumer<DeviceNotification, String> callback) {

        final String subscriptionId = UUID.randomUUID().toString();
        Set<NotificationSubscribeRequest> subscribeRequests = devices.stream()
                .map(device -> new NotificationSubscribeRequest(subscriptionId, device, names, timestamp))
                .collect(Collectors.toSet());
        Collection<CompletableFuture<Collection<DeviceNotification>>> futures = new ArrayList<>();
        for (NotificationSubscribeRequest sr : subscribeRequests) {
            CompletableFuture<Collection<DeviceNotification>> future = new CompletableFuture<>();
            Consumer<Response> responseConsumer = response -> {
                String resAction = response.getBody().getAction();
                if (resAction.equals(Action.NOTIFICATION_SUBSCRIBE_RESPONSE.name())) {
                    NotificationSubscribeResponse r = response.getBody().cast(NotificationSubscribeResponse.class);
                    future.complete(r.getNotifications());
                } else if (resAction.equals(Action.NOTIFICATION_EVENT.name())) {
                    NotificationEvent event = response.getBody().cast(NotificationEvent.class);
                    callback.accept(event.getNotification(), subscriptionId);
                } else {
                    logger.warn("Unknown action received from backend {}", resAction);
                }
            };
            futures.add(future);
            Request request = Request.newBuilder()
                    .withBody(sr)
                    .withPartitionKey(sr.getDevice())
                    .withSingleReply(false)
                    .build();
            rpcClient.call(request, responseConsumer);
        }

        CompletableFuture<List<DeviceNotification>> future = CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[futures.size()]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList()));
        return Pair.of(subscriptionId, future);
    }

    public void unsubscribe(String subId, Set<String> deviceGuids) {
        NotificationUnsubscribeRequest unsubscribeRequest = new NotificationUnsubscribeRequest(subId, deviceGuids);
        Request request = Request.newBuilder()
                .withBody(unsubscribeRequest)
                .build();
        rpcClient.push(request);
    }

    public DeviceNotification convertWrapperToNotification(DeviceNotificationWrapper notificationSubmit, DeviceVO device) {
        DeviceNotification notification = new DeviceNotification();
        notification.setId(Math.abs(new Random().nextInt()));
        notification.setDeviceGuid(device.getGuid());
        if (notificationSubmit.getTimestamp() == null) {
            notification.setTimestamp(timestampService.getDate());
        } else {
            notification.setTimestamp(notificationSubmit.getTimestamp());
        }
        notification.setNotification(notificationSubmit.getNotification());
        notification.setParameters(notificationSubmit.getParameters());
        return notification;
    }

    private List<DeviceNotification> processDeviceNotification(DeviceNotification notificationMessage, DeviceVO device) {
        List<DeviceNotification> notificationsToCreate = new ArrayList<>();
        if (notificationMessage.getNotification() != null) {
            switch (notificationMessage.getNotification()) {
                case SpecialNotifications.DEVICE_STATUS:
                    DeviceNotification deviceNotification = refreshDeviceStatusCase(notificationMessage, device);
                    notificationsToCreate.add(deviceNotification);
                    break;
                default:
                    break;
            }
        }

        notificationsToCreate.add(notificationMessage);
        return notificationsToCreate;

    }

    private DeviceNotification refreshDeviceStatusCase(DeviceNotification notificationMessage, DeviceVO device) {
        DeviceVO devicevo = deviceDao.findByUUID(device.getGuid());
        String status = ServerResponsesFactory.parseNotificationStatus(notificationMessage);
        devicevo.setStatus(status);
        return ServerResponsesFactory.createNotificationForDevice(devicevo, SpecialNotifications.DEVICE_UPDATE);
    }
}
