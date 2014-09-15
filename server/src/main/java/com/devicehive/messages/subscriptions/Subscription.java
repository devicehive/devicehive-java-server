package com.devicehive.messages.subscriptions;


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
}
