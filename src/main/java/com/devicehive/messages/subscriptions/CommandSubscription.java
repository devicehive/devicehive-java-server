package com.devicehive.messages.subscriptions;


import com.devicehive.messages.handler.HandlerCreator;

public class CommandSubscription extends Subscription<Long> {

    public CommandSubscription(Long deviceId, String subscriberId, HandlerCreator handlerCreator) {
        super(deviceId, subscriberId, handlerCreator);
    }

    public Long getDeviceId() {
        return getEventSource();
    }

    public String getSessionId() {
        return getSubscriberId();
    }

}

