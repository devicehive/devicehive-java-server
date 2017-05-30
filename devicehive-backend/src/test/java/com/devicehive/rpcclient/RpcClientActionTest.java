package com.devicehive.rpcclient;

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
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.rpc.*;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RpcClientActionTest extends AbstractSpringTest {

    @Autowired
    private RpcClient client;

    // subscriptions in _SubscribeIntegrationTest

    @Test
    public void testNotificationSearchAction() throws Exception {
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
    public void testCommandSearchAction() throws Exception {
        CommandSearchRequest searchRequest = new CommandSearchRequest();
        searchRequest.setId(Long.MAX_VALUE); // nonexistent id
        searchRequest.setGuid(UUID.randomUUID().toString()); // random guid

        Request request = Request.newBuilder()
                .withPartitionKey(searchRequest.getGuid())
                .withBody(searchRequest)
                .build();
        CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(request, future::complete);

        Response response = future.get(10, TimeUnit.SECONDS);
        CommandSearchResponse responseBody = (CommandSearchResponse) response.getBody();
        assertTrue(responseBody.getCommands().isEmpty());
    }

    @Test
    public void testCommandInsertAction() throws Exception {
        DeviceCommand command = new DeviceCommand();
        command.setCommand("test_command");
        command.setDeviceGuid(UUID.randomUUID().toString());
        CommandInsertRequest insertRequest = new CommandInsertRequest(command);

        Request request = Request.newBuilder()
                .withPartitionKey(insertRequest.getDeviceCommand().getDeviceGuid())
                .withBody(insertRequest)
                .build();
        CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(request, future::complete);

        Response response = future.get(10, TimeUnit.SECONDS);
        CommandInsertResponse responseBody = (CommandInsertResponse) response.getBody();
        assertNotNull(responseBody.getDeviceCommand());
    }

    @Test
    public void testCommandUpdateAction() throws Exception {
        DeviceCommand command = new DeviceCommand();
        command.setCommand("test_command");
        command.setResult(new JsonStringWrapper("{\"result\": \"OK\"}"));
        command.setDeviceGuid(UUID.randomUUID().toString());
        CommandUpdateRequest updateRequest = new CommandUpdateRequest(command);

        Request request = Request.newBuilder()
                .withPartitionKey(updateRequest.getDeviceCommand().getDeviceGuid())
                .withBody(updateRequest)
                .build();
        CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(request, future::complete);

        Response response = future.get(10, TimeUnit.SECONDS);
        assertNotNull(response);
    }

    @Test
    public void testNotificationInsertAction() throws Exception {
        DeviceNotification notification = new DeviceNotification();
        notification.setNotification("test_notification");
        notification.setDeviceGuid(UUID.randomUUID().toString());
        NotificationInsertRequest insertRequest = new NotificationInsertRequest(notification);

        Request request = Request.newBuilder()
                .withPartitionKey(insertRequest.getDeviceNotification().getDeviceGuid())
                .withBody(insertRequest)
                .build();
        CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(request, future::complete);

        Response response = future.get(10, TimeUnit.SECONDS);
        NotificationInsertResponse responseBody = (NotificationInsertResponse) response.getBody();
        assertNotNull(responseBody.getDeviceNotification());
    }

    @Test
    public void testListUserAction() throws Exception {
        ListUserRequest listUserRequest = new ListUserRequest();
        listUserRequest.setLogin(UUID.randomUUID().toString()); // nonexistent login

        Request request = Request.newBuilder()
                .withBody(listUserRequest)
                .build();
        CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(request, future::complete);

        Response response = future.get(10, TimeUnit.SECONDS);
        ListUserResponse responseBody = (ListUserResponse) response.getBody();
        assertNotNull(responseBody.getUsers().isEmpty());
    }

    @Test
    public void testListNetworkAction() throws Exception {
        ListNetworkRequest listNetworkRequest = new ListNetworkRequest();
        listNetworkRequest.setName(UUID.randomUUID().toString()); // nonexistent name

        Request request = Request.newBuilder()
                .withBody(listNetworkRequest)
                .build();
        CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(request, future::complete);

        Response response = future.get(10, TimeUnit.SECONDS);
        ListNetworkResponse responseBody = (ListNetworkResponse) response.getBody();
        assertNotNull(responseBody.getNetworks().isEmpty());
    }

    @Test
    public void testListDeviceAction() throws Exception {
        ListDeviceRequest deviceRequest = new ListDeviceRequest();
        deviceRequest.setName(UUID.randomUUID().toString()); // nonexistent name
        deviceRequest.setSortOrderAsc(false);

        Request request = Request.newBuilder()
                .withBody(deviceRequest)
                .build();
        CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(request, future::complete);

        Response response = future.get(10, TimeUnit.SECONDS);
        ListDeviceResponse responseBody = (ListDeviceResponse) response.getBody();
        assertNotNull(responseBody.getDevices().isEmpty());
    }
}
