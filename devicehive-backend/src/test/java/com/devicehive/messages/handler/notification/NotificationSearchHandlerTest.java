package com.devicehive.messages.handler.notification;

/*
 * #%L
 * DeviceHive Backend Logic
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

import com.devicehive.base.AbstractSpringTest;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.rpc.NotificationInsertRequest;
import com.devicehive.model.rpc.NotificationSearchRequest;
import com.devicehive.model.rpc.NotificationSearchResponse;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NotificationSearchHandlerTest extends AbstractSpringTest {

    @Autowired
    private RpcClient client;

    private String deviceId;
    private List<DeviceNotification> notifications;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deviceId = UUID.randomUUID().toString();
        // create notifications
        notifications = LongStream.range(0, 3)
                .mapToObj(i -> createNotification(i, deviceId))
                .collect(Collectors.toList());

        // insert notifications
        notifications.stream()
                .map(this::insertNotification)
                .forEach(this::waitForResponse);
    }

    @Test
    public void shouldNotFindSingleNotificationByIdAndDeviceId() throws Exception {
        NotificationSearchRequest searchRequest = new NotificationSearchRequest();
        searchRequest.setId(Long.MAX_VALUE); // nonexistent id
        searchRequest.setDeviceId(UUID.randomUUID().toString()); // random device id

        Request request = Request.newBuilder()
                .withPartitionKey(searchRequest.getDeviceId())
                .withBody(searchRequest)
                .build();
        CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(request, future::complete);

        Response response = future.get(10, TimeUnit.SECONDS);
        NotificationSearchResponse responseBody = (NotificationSearchResponse) response.getBody();
        assertTrue(responseBody.getNotifications().isEmpty());
    }

    @Test
    public void shouldFindSingleNotificationByIdAndDeviceId() throws Exception {
        NotificationSearchRequest searchRequest = new NotificationSearchRequest();
        searchRequest.setId(notifications.get(0).getId());
        searchRequest.setDeviceId(notifications.get(0).getDeviceId());

        Request request = Request.newBuilder()
                .withPartitionKey(notifications.get(0).getDeviceId())
                .withBody(searchRequest)
                .build();
        CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(request, future::complete);

        Response response = future.get(10, TimeUnit.SECONDS);
        NotificationSearchResponse responseBody = (NotificationSearchResponse) response.getBody();
        assertEquals(1, responseBody.getNotifications().size());
        assertEquals(notifications.get(0), responseBody.getNotifications().get(0));
    }

    @Test
    public void shouldHandleNotificationInsertAndQueryByDeviceIdAndNotificationName() throws Exception {
        NotificationSearchRequest searchRequest = new NotificationSearchRequest();
        searchRequest.setDeviceId(notifications.get(0).getDeviceId());
        searchRequest.setNames(Collections.singleton(notifications.get(0).getNotification()));

        Request request = Request.newBuilder()
                .withBody(searchRequest)
                .build();
        CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(request, future::complete);

        Response response = future.get(10, TimeUnit.SECONDS);
        NotificationSearchResponse responseBody = (NotificationSearchResponse) response.getBody();
        assertEquals(1, responseBody.getNotifications().size());
        assertEquals(notifications.get(0), responseBody.getNotifications().get(0));
    }

    private DeviceNotification createNotification(long id, String deviceId) {
        DeviceNotification notification = new DeviceNotification();
        notification.setId(id);
        notification.setTimestamp(Date.from(Instant.now()));
        notification.setDeviceId(deviceId);
        notification.setNotification("SOME TEST DATA_" + id);
        notification.setParameters(new JsonStringWrapper("{\"param1\":\"value1\",\"param2\":\"value2\"}"));
        return notification;
    }

    private CompletableFuture<Response> insertNotification(DeviceNotification notification) {
        final CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(Request.newBuilder()
                .withBody(new NotificationInsertRequest(notification))
                .withPartitionKey(notification.getDeviceId()) // partitioning by device id
                .build(), future::complete);
        return future;
    }

    private Response waitForResponse(CompletableFuture<Response> future) {
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
