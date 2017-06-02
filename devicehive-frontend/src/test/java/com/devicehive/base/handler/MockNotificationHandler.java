package com.devicehive.base.handler;

/*
 * #%L
 * DeviceHive Frontend Logic
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
                        .filter(n -> n.getDeviceId().equals(body.getDeviceId()))
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
                        .filter(n -> n.getDeviceId().equals(body.getDevice()))
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
