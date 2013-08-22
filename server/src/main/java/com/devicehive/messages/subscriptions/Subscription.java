package com.devicehive.messages.subscriptions;


import com.devicehive.messages.handler.HandlerCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Subscription<EventSource> {

    private static final Logger logger = LoggerFactory.getLogger(Subscription.class);

    private EventSource eventSource;

    private String subscriberId;

    private HandlerCreator handlerCreator;


    public Subscription(EventSource eventSource, String subscriberId, HandlerCreator handlerCreator) {
        this.eventSource = eventSource;
        this.subscriberId = subscriberId;
        this.handlerCreator = handlerCreator;
    }

    EventSource getEventSource() {
        return eventSource;
    }

    String getSubscriberId() {
        return subscriberId;
    }

    public HandlerCreator getHandlerCreator() {
        return handlerCreator;
    }
}
