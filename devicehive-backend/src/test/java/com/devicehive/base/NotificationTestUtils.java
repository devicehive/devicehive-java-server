package com.devicehive.base;

import com.devicehive.model.DeviceNotification;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.rpc.NotificationInsertRequest;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class NotificationTestUtils {

    public static DeviceNotification generateNotification(long id, long networkId, String deviceId) {
        DeviceNotification notification = new DeviceNotification();
        notification.setId(id);
        notification.setTimestamp(Date.from(Instant.now()));
        notification.setNetworkId(networkId);
        notification.setDeviceId(deviceId);
        notification.setNotification("SOME TEST DATA_" + id);
        notification.setParameters(new JsonStringWrapper("{\"param1\":\"value1\",\"param2\":\"value2\"}"));
        return notification;
    }

    public static CompletableFuture<Response> insertNotification(RpcClient client, DeviceNotification notification) {
        final CompletableFuture<Response> future = new CompletableFuture<>();
        client.call(Request.newBuilder()
                .withBody(new NotificationInsertRequest(notification))
                .withPartitionKey(notification.getDeviceId()) // partitioning by device id
                .build(), future::complete);
        return future;
    }

    public static Response waitForResponse(CompletableFuture<Response> future) {
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
