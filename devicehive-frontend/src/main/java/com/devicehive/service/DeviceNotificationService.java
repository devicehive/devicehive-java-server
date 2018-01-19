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

import com.devicehive.api.RequestResponseMatcher;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.SpecialNotifications;
import com.devicehive.model.eventbus.Filter;
import com.devicehive.model.eventbus.events.NotificationEvent;
import com.devicehive.model.rpc.*;
import com.devicehive.model.wrappers.DeviceNotificationWrapper;
import com.devicehive.service.helpers.LongIdGenerator;
import com.devicehive.service.helpers.ResponseConsumer;
import com.devicehive.service.time.TimestampService;
import com.devicehive.shim.api.Action;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.util.HiveValidator;
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

    private final TimestampService timestampService;
    private final RpcClient rpcClient;
    private final HiveValidator hiveValidator;
    private final LongIdGenerator idGenerator;
    private final RequestResponseMatcher requestResponseMatcher;

    @Autowired
    public DeviceNotificationService(TimestampService timestampService,
                                     RpcClient rpcClient,
                                     HiveValidator hiveValidator,
                                     LongIdGenerator idGenerator,
                                     RequestResponseMatcher requestResponseMatcher) {
        this.timestampService = timestampService;
        this.rpcClient = rpcClient;
        this.hiveValidator = hiveValidator;
        this.idGenerator = idGenerator;
        this.requestResponseMatcher = requestResponseMatcher;
    }

    public CompletableFuture<Optional<DeviceNotification>> findOne(Long id, String deviceId) {
        NotificationSearchRequest searchRequest = new NotificationSearchRequest();
        searchRequest.setId(id);
        searchRequest.setDeviceIds(Collections.singleton(deviceId));

        CompletableFuture<Response> future = new CompletableFuture<>();
        rpcClient.call(Request.newBuilder()
                .withBody(searchRequest)
                .withPartitionKey(deviceId)
                .build(), new ResponseConsumer(future));
        return future.thenApply(r -> ((NotificationSearchResponse) r.getBody()).getNotifications().stream().findFirst());
    }

    public CompletableFuture<List<DeviceNotification>> find(NotificationSearchRequest request) {
        
        return find(request.getDeviceIds(), request.getNames(), request.getTimestampStart(), request.getTimestampEnd(),
                request.getSortField(), request.getSortOrder(), request.getTake(), request.getSkip());
    }

    @SuppressWarnings("unchecked")
    public CompletableFuture<List<DeviceNotification>> find(Set<String> deviceIds, Set<String> names, Date timestampSt,
            Date timestampEnd, String sortField, String sortOrder, Integer take, Integer skip) {
        NotificationSearchRequest searchRequest = new NotificationSearchRequest();
        searchRequest.setDeviceIds(deviceIds);
        searchRequest.setNames(names);
        searchRequest.setTimestampStart(timestampSt);
        searchRequest.setTimestampEnd(timestampEnd);
        searchRequest.setSortField(sortField);
        searchRequest.setSortOrder(sortOrder);
        searchRequest.setTake(take);
        searchRequest.setSkip(skip);
        
        CompletableFuture<Response> future = new CompletableFuture<>();
        rpcClient.call(Request.newBuilder()
                .withBody(searchRequest)
                .withPartitionKey(searchRequest.getDeviceId())
                .build(), new ResponseConsumer(future));

        // CompletableFuture<Response> => CompletableFuture<List<DeviceNotification>>
        return future.thenApply(v -> v.getBody().cast(NotificationSearchResponse.class).getNotifications());
    }

    public CompletableFuture<DeviceNotification> insert(final DeviceNotification notification,
                                                        final DeviceVO device) {
        hiveValidator.validate(notification);
        CompletableFuture<Response> future = new CompletableFuture<>();
        rpcClient.call(Request.newBuilder()
                .withBody(new NotificationInsertRequest(notification))
                .withPartitionKey(device.getDeviceId())
                .build(), new ResponseConsumer(future));
        
        return future.thenApply(r -> r.getBody().cast(NotificationInsertResponse.class).getDeviceNotification());
    }

    public Pair<Long, CompletableFuture<List<DeviceNotification>>> subscribe(
            final Set<Filter> filters,
            final Set<String> names,
            final Date timestamp,
            final BiConsumer<DeviceNotification, Long> callback) {

        final Long subscriptionId = idGenerator.generate();
        Set<NotificationSubscribeRequest> subscribeRequests = filters.stream()
                .map(filter -> new NotificationSubscribeRequest(subscriptionId, filter, names, timestamp))
                .collect(Collectors.toSet());
        Collection<CompletableFuture<Collection<DeviceNotification>>> futures = new ArrayList<>();
        for (NotificationSubscribeRequest sr : subscribeRequests) {
            CompletableFuture<Collection<DeviceNotification>> future = new CompletableFuture<>();
            Consumer<Response> responseConsumer = response -> {
                Action resAction = response.getBody().getAction();
                if (resAction.equals(Action.NOTIFICATION_SUBSCRIBE_RESPONSE)) {
                    NotificationSubscribeResponse r = response.getBody().cast(NotificationSubscribeResponse.class);
                    requestResponseMatcher.addSubscription(subscriptionId, response.getCorrelationId());
                    future.complete(r.getNotifications());
                } else if (resAction.equals(Action.NOTIFICATION_EVENT)) {
                    NotificationEvent event = response.getBody().cast(NotificationEvent.class);
                    callback.accept(event.getNotification(), subscriptionId);
                } else {
                    logger.warn("Unknown action received from backend {}", resAction);
                }
            };
            futures.add(future);
            Request request = Request.newBuilder()
                    .withBody(sr)
                    .withPartitionKey(sr.getFilter().getFirstKey())
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

    public CompletableFuture<Set<Long>> unsubscribe(Set<Long> subIds) {
        NotificationUnsubscribeRequest unsubscribeRequest = new NotificationUnsubscribeRequest(subIds);
        Request request = Request.newBuilder()
                .withBody(unsubscribeRequest)
                .build();
        CompletableFuture<Set<Long>> future = new CompletableFuture<>();
        Consumer<Response> responseConsumer = response -> {
            Action resAction = response.getBody().getAction();
            if (resAction.equals(Action.NOTIFICATION_UNSUBSCRIBE_RESPONSE)) {
                future.complete(response.getBody().cast(NotificationUnsubscribeResponse.class).getSubscriptionIds());
                subIds.forEach(requestResponseMatcher::removeSubscription);
            } else {
                logger.warn("Unknown action received from backend {}", resAction);
            }
        };
        rpcClient.call(request, responseConsumer);
        return future;
    }

    public DeviceNotification convertWrapperToNotification(DeviceNotificationWrapper notificationSubmit, DeviceVO device) {
        DeviceNotification notification = new DeviceNotification();
        notification.setId(Math.abs(new Random().nextInt()));
        notification.setDeviceId(device.getDeviceId());
        notification.setNetworkId(device.getNetworkId());
        notification.setDeviceTypeId(device.getDeviceTypeId());
        if (notificationSubmit.getTimestamp() == null) {
            notification.setTimestamp(timestampService.getDate());
        } else {
            notification.setTimestamp(notificationSubmit.getTimestamp());
        }
        notification.setNotification(notificationSubmit.getNotification());
        notification.setParameters(notificationSubmit.getParameters());
        return notification;
    }

}
