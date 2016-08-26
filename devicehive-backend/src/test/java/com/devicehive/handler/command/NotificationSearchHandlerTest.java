package com.devicehive.handler.command;

import com.devicehive.base.AbstractSpringTest;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.rpc.NotificationInsertRequest;
import com.devicehive.model.rpc.NotificationSearchRequest;
import com.devicehive.model.rpc.NotificationSearchResponse;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

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
        Assert.assertTrue(responseBody.getNotifications().isEmpty());
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
        Assert.assertEquals(1, responseBody.getNotifications().size());
        Assert.assertEquals(notifications.get(0), responseBody.getNotifications().get(0));
    }

    private DeviceNotification createNotification(long id, String guid) {
        DeviceNotification notification = new DeviceNotification();
        notification.setId(id);
        notification.setTimestamp(Date.from(Instant.now()));
        notification.setDeviceGuid(guid);
        notification.setNotification("SOME TEST DATA");
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
