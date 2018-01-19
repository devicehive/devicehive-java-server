package com.devicehive.eventbus;

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

import com.devicehive.model.eventbus.Filter;
import com.devicehive.model.eventbus.FilterRegistry;
import com.devicehive.model.eventbus.Subscriber;
import com.devicehive.model.eventbus.events.Event;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.MessageDispatcher;
import com.devicehive.vo.DeviceVO;

/**
 * Central class for interaction with devicehive-backend subscription mechanism.
 * Provides basic interfaces and operations for subscription, unsubscription and publishing of events.
 */
public class EventBus {

    private final FilterRegistry registry;
    private final MessageDispatcher dispatcher;

    /**
     * Creates new instance of EventBus
     * @param dispatcher - interface, that controls message delivery strategy
     */
    public EventBus(MessageDispatcher dispatcher, FilterRegistry registry) {
        this.dispatcher = dispatcher;
        this.registry = registry;
    }

    public void subscribe(Filter filter, Subscriber subscriber) {
        registry.register(filter, subscriber);
    }

    public void unsubscribe(Subscriber subscriber) {
        registry.unregister(subscriber);
    }

    public void publish(Event event) {
        event.getApplicableFilters()
                .stream()
                .flatMap(filter -> registry.getSubscribers(filter).stream())
                .forEach(subscriber -> {
                    Response response = Response.newBuilder()
                            .withBody(event)
                            .withCorrelationId(subscriber.getCorrelationId())
                            .withLast(false)
                            .buildSuccess();
                    dispatcher.send(subscriber.getReplyTo(), response);
                });
    }

    public void unsubscribeDevice(DeviceVO device) {
         registry.unregisterDevice(device);
    }
}
