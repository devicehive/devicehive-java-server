package com.devicehive.handler.notification;

import com.devicehive.base.AbstractSpringTest;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.rpc.NotificationInsertRequest;
import com.devicehive.service.HazelcastService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class NotificationInsertHandlerTest extends AbstractSpringTest {

    @Autowired
    private RpcClient client;

    @Autowired
    private HazelcastService hazelcastService;

    @Test
    public void testInsertNotification() throws ExecutionException, InterruptedException, TimeoutException {
        final String corelationId = UUID.randomUUID().toString();
        final String guid = UUID.randomUUID().toString();
        final long id = System.nanoTime();

        DeviceNotification originalNotification = new DeviceNotification();
        originalNotification.setTimestamp(Date.from(Instant.now()));
        originalNotification.setId(id);
        originalNotification.setDeviceGuid(guid);
        originalNotification.setNotification("SOME TEST DATA");
        originalNotification.setParameters(new JsonStringWrapper("{\"param1\":\"value1\",\"param2\":\"value2\"}"));

        final CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(Request.newBuilder()
                .withBody(new NotificationInsertRequest(originalNotification))
                .withPartitionKey(originalNotification.getDeviceGuid())
                .build(), future::complete);

        Response response = future.get(10, TimeUnit.SECONDS);
        Assert.assertEquals(corelationId, response.getCorrelationId());
        Assert.assertTrue(hazelcastService.find(id, guid, DeviceNotification.class)
                .filter(notification -> notification.equals(originalNotification))
                .isPresent());
    }
}
