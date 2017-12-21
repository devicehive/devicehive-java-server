package com.devicehive.base;

/*
 * #%L
 * DeviceHive Backend Logic
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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

    public static DeviceNotification generateNotification(long id, long networkId, long deviceTypeId, String deviceId) {
        DeviceNotification notification = new DeviceNotification();
        notification.setId(id);
        notification.setTimestamp(Date.from(Instant.now()));
        notification.setNetworkId(networkId);
        notification.setDeviceTypeId(deviceTypeId);
        notification.setDeviceId(deviceId);
        notification.setNotification("SOME TEST DATA_" + id);
        notification.setParameters(new JsonStringWrapper("{\"param1\":\"value1\",\"param2\":\"value2\"}"));
        return notification;
    }

    public static DeviceNotification generateNotification(long id, String deviceId) {
        DeviceNotification notification = new DeviceNotification();
        notification.setId(id);
        notification.setTimestamp(Date.from(Instant.now()));
        notification.setNetworkId(0L);
        notification.setDeviceTypeId(0L);
        notification.setDeviceId(deviceId);
        notification.setNotification("SOME TEST DATA");
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
