package com.devicehive.handler.notification;

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

import com.devicehive.eventbus.EventBus;
import com.devicehive.model.eventbus.Subscriber;
import com.devicehive.model.eventbus.Subscription;
import com.devicehive.model.rpc.*;
import com.devicehive.shim.api.Action;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Set;

public class NotificationUnsubscribeRequestHandler implements RequestHandler {

    @Autowired
    private EventBus eventBus;

    @Override
    public Response handle(Request request) {
        NotificationUnsubscribeRequest body = (NotificationUnsubscribeRequest) request.getBody();
        validate(body);

        if (body.getSubscriptionId() != null) {
            Subscriber subscriber = new Subscriber(body.getSubscriptionId(), request.getReplyTo(),
                    request.getCorrelationId());
            eventBus.unsubscribe(subscriber);

            NotificationUnsubscribeResponse unsubscribeResponse =
                    new NotificationUnsubscribeResponse(body.getSubscriptionId(), null);

            return Response.newBuilder()
                    .withBody(unsubscribeResponse)
                    .withCorrelationId(request.getCorrelationId())
                    .buildSuccess();
        } else if (body.getDeviceIds() != null) {
            Set<Subscription> subscriptions = new HashSet<>();
            Set<Subscriber> subscribers = new HashSet<>();

            for (String name : body.getDeviceIds()) {
                Subscription subscription = new Subscription(Action.NOTIFICATION_EVENT.name(), name);
                subscriptions.add(subscription);
            }

            subscriptions.forEach(subscription -> subscribers.addAll(eventBus.getSubscribers(subscription)));
            subscribers.forEach(subscriber -> eventBus.unsubscribe(subscriber));

            NotificationUnsubscribeResponse unsubscribeResponse =
                    new NotificationUnsubscribeResponse(null, body.getDeviceIds());
            return Response.newBuilder()
                    .withBody(unsubscribeResponse)
                    .withCorrelationId(request.getCorrelationId())
                    .buildSuccess();
        } else {
            throw new IllegalArgumentException("Both subscription id and device ids are null");
        }
    }

    private void validate(NotificationUnsubscribeRequest request) {
        Assert.notNull(request, "Request body is null");
    }
}
