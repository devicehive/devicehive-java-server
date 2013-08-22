package com.devicehive.messages.subscriptions;


import com.devicehive.messages.handler.HandlerCreator;

public class CommandUpdateSubscription extends Subscription<Long> {

    public CommandUpdateSubscription(Long commandId, String subscriberId, HandlerCreator handlerCreator) {
        super(commandId, subscriberId, handlerCreator);
    }

    public Long getCommandId() {
        return getEventSource();
    }

    public String getSessionId() {
        return getSubscriberId();
    }
}
