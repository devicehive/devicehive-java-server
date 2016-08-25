package com.devicehive.handler.notification;

import com.devicehive.model.DeviceNotification;
import com.devicehive.model.rpc.NotificationInsertRequest;
import com.devicehive.service.HazelcastService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;

public class NotificationInsertHandler implements RequestHandler {

    @Autowired
    private HazelcastService hazelcastService;

    @Override
    public Response handle(Request request) {
        DeviceNotification notification = ((NotificationInsertRequest) request.getBody()).getDeviceNotification();
        hazelcastService.store(notification);

        // FIXME: add notification routing

        return Response.newBuilder()
                .withLast(true)
                .buildSuccess();
    }
}
