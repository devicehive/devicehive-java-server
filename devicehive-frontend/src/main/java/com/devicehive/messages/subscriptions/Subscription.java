package com.devicehive.messages.subscriptions;

import com.devicehive.messages.handler.HandlerCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Subscription<EventSource, T> {

    private static final Logger logger = LoggerFactory.getLogger(Subscription.class);

    private EventSource eventSource;

    private String subscriptionId;

    private HandlerCreator<T> handlerCreator;


    public Subscription(EventSource eventSource, String subscriptionId, HandlerCreator<T> handlerCreator) {
        this.eventSource = eventSource;
        this.subscriptionId = subscriptionId;
        this.handlerCreator = handlerCreator;
    }

    EventSource getEventSource() {
        return eventSource;
    }

    public String getSubscriptionId() {
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
