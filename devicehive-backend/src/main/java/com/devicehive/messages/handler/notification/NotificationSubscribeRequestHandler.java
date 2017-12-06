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
import com.devicehive.service.HazelcastService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.*;

@Component
public class NotificationSubscribeRequestHandler implements RequestHandler {

    public static final int LIMIT = 100;

    private EventBus eventBus;
    private HazelcastService hazelcastService;

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
                .map(t -> hazelcastService.find(filter.getDeviceId(),
                        Collections.singleton(filter.getNetworkId()),
                        Collections.singleton(filter.getDeviceTypeId()),
                        names, LIMIT, t, null, false, null, DeviceNotification.class))
                .orElse(Collections.emptyList());
    }

}
