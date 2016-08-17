package com.devicehive.messages.subscriptions;

/*
 * #%L
 * DeviceHive Java Server Common business logic
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


import com.devicehive.messages.handler.HandlerCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public abstract class Subscription<EventSource, T> {

    private static final Logger logger = LoggerFactory.getLogger(Subscription.class);

    private EventSource eventSource;

    private UUID subscriptionId;

    private HandlerCreator<T> handlerCreator;


    public Subscription(EventSource eventSource, UUID subscriptionId, HandlerCreator<T> handlerCreator) {
        this.eventSource = eventSource;
        this.subscriptionId = subscriptionId;
        this.handlerCreator = handlerCreator;
    }

    EventSource getEventSource() {
        return eventSource;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public HandlerCreator<T> getHandlerCreator() {
        return handlerCreator;
    }

    @Override
    public String toString() {
        return "Subscription{" +
                "eventSource=" + eventSource +
                ", subscriptionId=" + subscriptionId +
                ", handlerCreator=" + handlerCreator +
                '}';
    }
}
