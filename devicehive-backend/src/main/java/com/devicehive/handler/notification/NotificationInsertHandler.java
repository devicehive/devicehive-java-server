package com.devicehive.handler.notification;

import com.devicehive.eventbus.EventBus;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.eventbus.events.NotificationEvent;
import com.devicehive.model.rpc.NotificationInsertRequest;
import com.devicehive.model.rpc.NotificationInsertResponse;
import com.devicehive.service.HazelcastService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;

public class NotificationInsertHandler implements RequestHandler {

    private HazelcastService hazelcastService;

    private EventBus eventBus;

    @Autowired
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Autowired
    public void setHazelcastService(HazelcastService hazelcastService) {
        this.hazelcastService = hazelcastService;
    }

    @Override
    public Response handle(Request request) {
        DeviceNotification notification = ((NotificationInsertRequest) request.getBody()).getDeviceNotification();
        hazelcastService.store(notification);

        NotificationEvent notificationEvent = new NotificationEvent(notification);
        eventBus.publish(notificationEvent);

        NotificationInsertResponse payload = new NotificationInsertResponse(notification);
        return Response.newBuilder()
                .withBody(payload)
                .buildSuccess();
    }
}
