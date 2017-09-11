package com.devicehive.handler;

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

import com.devicehive.eventbus.EventBus;
import com.devicehive.eventbus.FilterRegistry;
import com.devicehive.model.eventbus.Filter;
import com.devicehive.model.eventbus.Subscription;
import com.devicehive.model.rpc.DeviceCreateRequest;
import com.devicehive.model.rpc.DeviceCreateResponse;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.vo.DeviceVO;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.Set;

public class DeviceCreateHandler implements RequestHandler {

    @Autowired
    private EventBus eventBus;

    @Autowired
    private FilterRegistry filterRegistry;

    @Override
    public Response handle(Request request) {
        final DeviceCreateRequest req = (DeviceCreateRequest) request.getBody();
        final DeviceVO device = req.getDevice();
        final Set<Pair<Long, Filter>> subs = filterRegistry.getSubscriptions(device.getNetworkId());

        if (req.getOldNetwork() != null && !req.getOldNetwork().equals(device.getNetworkId())) {
            eventBus.getAllSubscriptions().stream()
                    .filter(subscription -> subscription.getEntityId().equals(device.getDeviceId()))
                    .forEach(eventBus::unsubscribe);
        }

        if (subs != null) {
            subs.forEach(sub -> {
                Filter filter = sub.getRight();
                Set<String> names = filter.getNames();
                String eventName = filter.getEventName();

                Set<Subscription> subscriptions = new HashSet<>();
                if (CollectionUtils.isEmpty(names)) {
                    Subscription subscription = new Subscription(eventName, device.getDeviceId());
                    subscriptions.add(subscription);
                } else {
                    for (String name : names) {
                        Subscription subscription = new Subscription(eventName, device.getDeviceId(), name);
                        subscriptions.add(subscription);
                    }
                }

                subscriptions.forEach(subscription ->
                        eventBus.subscribe(eventBus.getSubscriber(sub.getLeft()), subscription));
            });
        }

        return Response.newBuilder()
                .withBody(new DeviceCreateResponse())
                .withCorrelationId(request.getCorrelationId())
                .buildSuccess();
    }
}
