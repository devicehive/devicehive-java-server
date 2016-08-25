package com.devicehive.service;

import com.devicehive.model.rpc.NotificationSearchRequest;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Consumer;

@Lazy
@Component
public class DeviceNotificationServiceImpl {

    @Autowired
    private RpcClient rpcClient;

    public void find(Long notificationId, String deviceGuid, Consumer<Response> callback) {
        NotificationSearchRequest requestBody = new NotificationSearchRequest() {{
            setId(notificationId);
            setGuid(deviceGuid);
            setDevices(Collections.EMPTY_SET);
            setNames(Collections.EMPTY_SET);
            setTimestamp(null);
            setTake(1);
        }};
        Request request = Request.newBuilder()
                .withPartitionKey(deviceGuid)
                .withCorrelationId(UUID.randomUUID().toString())
                .withBody(requestBody)
                .build();

        rpcClient.call(request, callback);
    }

    public void find(Collection<String> deviceGuids, Collection<String> notificationNames, Date fromTimestamp, Integer take, Consumer<Response> callback) {
        //TODO should be several requests here with completable stage and post processing in the callback to call client's callback function.
        //TODO lacks of partition key - should be device id
        Request request = Request.newBuilder()
                .withCorrelationId(UUID.randomUUID().toString())
                .withBody(new NotificationSearchRequest() {{
                    setDevices(new HashSet<>(deviceGuids));
                    setNames(new HashSet<>(notificationNames));
                    setTimestamp(fromTimestamp);
                    setTake(take);
                }})
                .build();
        rpcClient.call(request, callback);
    }
}
