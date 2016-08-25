package com.devicehive.eventbus;

import com.devicehive.model.eventbus.Subscriber;
import com.devicehive.model.eventbus.Subscription;
import com.devicehive.model.eventbus.events.Event;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.MessageDispatcher;

/**
 * Central class for interaction with devicehive-backend subscription mechanism.
 * Provides basic interfaces and operations for subscription, unsubscription and publishing of events.
 */
public class EventBus {

    private final SubscriberRegistry registry = new SubscriberRegistry();
    private final MessageDispatcher dispatcher;

    /**
     * Creates new instance of EventBus
     * @param dispatcher - interface, that controls message delivery strategy
     */
    public EventBus(MessageDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void subscribe(Subscriber subscriber, Subscription subscription) {
        registry.register(subscriber, subscription);
    }

    public void unsubscribe(Subscriber subscriber) {
        registry.unregister(subscriber);
    }

    public void publish(Event event) {
        event.getApplicableSubscriptions()
                .stream()
                .flatMap(subscription -> registry.getSubscribers(subscription).stream())
                .forEach(subscriber -> {
                    Response response = Response.newBuilder()
                            .withBody(event)
                            .withCorrelationId(subscriber.getId())
                            .withLast(false)
                            .buildSuccess();
                    dispatcher.send(subscriber.getReplyTo(), response);
                });
    }
}
