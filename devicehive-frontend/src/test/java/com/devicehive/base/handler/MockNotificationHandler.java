package com.devicehive.base.handler;

import com.devicehive.model.DeviceNotification;
import com.devicehive.model.SpecialNotifications;
import com.devicehive.model.rpc.*;
import com.devicehive.shim.api.Body;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.server.RequestHandler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class MockNotificationHandler {

    private Set<DeviceNotification> notificationSet = new HashSet<>();

    public void handle (RequestHandler handler) {
        when(handler.handle(any(Request.class))).thenAnswer(invocation -> {
            Request request = invocation.getArgumentAt(0, Request.class);
            if (request.getBody() instanceof NotificationInsertRequest) {
                DeviceNotification notification = ((NotificationInsertRequest) request.getBody()).getDeviceNotification();

                if (!notification.getNotification().equals(SpecialNotifications.DEVICE_ADD)) {
                    notificationSet.add(notification);
                }

                NotificationInsertResponse payload = new NotificationInsertResponse(notification);
                return com.devicehive.shim.api.Response.newBuilder()
                        .withBody(payload)
                        .buildSuccess();
            } else if (request.getBody() instanceof NotificationSearchRequest) {
                NotificationSearchRequest body = (NotificationSearchRequest) request.getBody();
                List<DeviceNotification> notifications = notificationSet
                        .stream()
                        .filter(n -> n.getDeviceGuid().equals(body.getGuid()))
                        .collect(Collectors.toList());

                NotificationSearchResponse subscribeResponse = new NotificationSearchResponse(notifications);

                return com.devicehive.shim.api.Response.newBuilder()
                        .withBody(subscribeResponse)
                        .withCorrelationId(request.getCorrelationId())
                        .buildSuccess();
            } else if (request.getBody() instanceof NotificationSubscribeRequest) {
                NotificationSubscribeRequest body = (NotificationSubscribeRequest) request.getBody();
                Set<DeviceNotification> notifications = notificationSet
                        .stream()
                        .filter(n -> n.getDeviceGuid().equals(body.getDevice()))
                        .collect(Collectors.toSet());
                NotificationSubscribeResponse subscribeResponse = new NotificationSubscribeResponse(body.getSubscriptionId(), notifications);

                return com.devicehive.shim.api.Response.newBuilder()
                        .withBody(subscribeResponse)
                        .withCorrelationId(request.getCorrelationId())
                        .buildSuccess();
            } else {
                return com.devicehive.shim.api.Response.newBuilder()
                        .withBody(new Body("") {
                        })
                        .buildSuccess();
            }
        });
    }
}
