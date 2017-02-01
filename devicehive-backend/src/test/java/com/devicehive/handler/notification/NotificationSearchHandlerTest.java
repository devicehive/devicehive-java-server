package com.devicehive.handler.notification;

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

    private String guid;
    private List<DeviceNotification> notifications;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        guid = UUID.randomUUID().toString();
        // create notifications
        notifications = LongStream.range(0, 3)
                .mapToObj(i -> createNotification(i, guid))
                .collect(Collectors.toList());

        // insert notifications
        notifications.stream()
                .map(this::insertNotification)
                .forEach(this::waitForResponse);
    }

    @Test
    public void shouldNotFindSingleNotificationByIdAndGuid() throws Exception {
        NotificationSearchRequest searchRequest = new NotificationSearchRequest();
        searchRequest.setId(Long.MAX_VALUE); // nonexistent id
        searchRequest.setGuid(UUID.randomUUID().toString()); // random guid

        Request request = Request.newBuilder()
                .withPartitionKey(searchRequest.getGuid())
                .withBody(searchRequest)
                .build();
        CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(request, future::complete);

        Response response = future.get(10, TimeUnit.SECONDS);
        NotificationSearchResponse responseBody = (NotificationSearchResponse) response.getBody();
        assertTrue(responseBody.getNotifications().isEmpty());
    }

    @Test
    public void shouldFindSingleNotificationByIdAndGuid() throws Exception {
        NotificationSearchRequest searchRequest = new NotificationSearchRequest();
        searchRequest.setId(notifications.get(0).getId());
        searchRequest.setGuid(notifications.get(0).getDeviceGuid());

        Request request = Request.newBuilder()
                .withPartitionKey(notifications.get(0).getDeviceGuid())
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
    public void shouldHandleNotificationInsertAndQueryByDeviceGuidAndNotificationName() throws Exception {
        NotificationSearchRequest searchRequest = new NotificationSearchRequest();
        searchRequest.setGuid(notifications.get(0).getDeviceGuid());
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

    private DeviceNotification createNotification(long id, String guid) {
        DeviceNotification notification = new DeviceNotification();
        notification.setId(id);
        notification.setTimestamp(Date.from(Instant.now()));
        notification.setDeviceGuid(guid);
        notification.setNotification("SOME TEST DATA_" + id);
        notification.setParameters(new JsonStringWrapper("{\"param1\":\"value1\",\"param2\":\"value2\"}"));
        return notification;
    }

    private CompletableFuture<Response> insertNotification(DeviceNotification notification) {
        final CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(Request.newBuilder()
                .withBody(new NotificationInsertRequest(notification))
                .withPartitionKey(notification.getDeviceGuid()) // partitioning by guid
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
