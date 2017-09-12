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

import com.devicehive.model.eventbus.Subscriber;
import com.devicehive.model.eventbus.Subscription;
import com.devicehive.model.eventbus.events.Event;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.MessageDispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Central class for interaction with devicehive-backend subscription mechanism.
 * Provides basic interfaces and operations for subscription, unsubscription and publishing of events.
 */
@Component
public class EventBus {

    private final SubscriberRegistry registry = new SubscriberRegistry();
    private final MessageDispatcher dispatcher;

    /**
     * Creates new instance of EventBus
     * @param dispatcher - interface, that controls message delivery strategy
     */
    @Autowired
    public EventBus(MessageDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void subscribe(Subscriber subscriber, Subscription subscription) {
        registry.register(subscriber, subscription);
    }

    public void unsubscribe(Subscriber subscriber) {
        registry.unregister(subscriber);
    }

    public void unsubscribe(Subscription subscription) {
        registry.unregister(subscription);
    }

    public Collection<Subscriber> getSubscribers(Subscription subscription) {
        return registry.getSubscribers(subscription);
    }

    public Subscriber getSubscriber(Long subscriptionId) {
        return registry.getSubscriber(subscriptionId);
    }

    public Collection<Subscription> getSubscriptions(Subscriber subscriber) {
       return registry.getSubscriptions(subscriber);
    }

    public Collection<Subscription> getAllSubscriptions() {
        return registry.getAllSubscriptions();
    }

    public void publish(Event event) {
        event.getApplicableSubscriptions()
                .stream()
                .flatMap(subscription -> registry.getSubscribers(subscription).stream())
                .forEach(subscriber -> {
                    Response response = Response.newBuilder()
                            .withBody(event)
                            .withCorrelationId(subscriber.getCorrelationId())
                            .withLast(false)
                            .buildSuccess();
                    dispatcher.send(subscriber.getReplyTo(), response);
                });
    }
}
