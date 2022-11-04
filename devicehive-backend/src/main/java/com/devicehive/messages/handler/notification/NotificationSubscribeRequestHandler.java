package com.devicehive.messages.handler.notification;

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
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.eventbus.Filter;
import com.devicehive.model.eventbus.Subscriber;
import com.devicehive.model.rpc.NotificationSubscribeRequest;
import com.devicehive.model.rpc.NotificationSubscribeResponse;
import com.devicehive.service.cache.notification.NotificationCacheService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

@Component
public class NotificationSubscribeRequestHandler implements RequestHandler {

    public static final int LIMIT = 100;

    private EventBus eventBus;
    private NotificationCacheService notificationCacheService;

    @Autowired
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Autowired
    public void setNotificationCacheService(NotificationCacheService notificationCacheService) {
        this.notificationCacheService = notificationCacheService;
    }

    @Override
    public Response handle(Request request) {
        NotificationSubscribeRequest body = (NotificationSubscribeRequest) request.getBody();
        validate(body);

        Subscriber subscriber = new Subscriber(body.getSubscriptionId(), request.getReplyTo(), request.getCorrelationId());
        Filter filter = body.getFilter();

        eventBus.subscribe(filter, subscriber);

        Collection<DeviceNotification> notifications = findNotifications(filter, body.getNames(), body.getTimestamp());
        NotificationSubscribeResponse subscribeResponse = new NotificationSubscribeResponse(body.getSubscriptionId(), notifications);

        return Response.newBuilder()
                       .withBody(subscribeResponse)
                       .withLast(false)
                       .withCorrelationId(request.getCorrelationId())
                       .buildSuccess();
    }

    private void validate(NotificationSubscribeRequest request) {
        Assert.notNull(request, "Request body is null");
        Assert.notNull(request.getFilter(), "Filter is null");
        Assert.notNull(request.getSubscriptionId(), "Subscription id not provided");
    }

    private Collection<DeviceNotification> findNotifications(Filter filter, Collection<String> names, Date timestamp) {
        return Optional.ofNullable(timestamp)
                       .map(t -> notificationCacheService.find(
                               filter.getDeviceId(),
                               Collections.singleton(filter.getNetworkId()),
                               Collections.singleton(filter.getDeviceTypeId()),
                               names, LIMIT, t))
                       .orElse(Collections.emptyList());
    }

}
