package com.devicehive.messages.subscriptions;


import com.devicehive.messages.handler.HandlerCreator;

import java.util.UUID;

public class CommandUpdateSubscription extends Subscription<Long> {

    public CommandUpdateSubscription(Long commandId, UUID subscriberId, HandlerCreator handlerCreator) {
        super(commandId, subscriberId.toString(), handlerCreator);
    }

    public Long getCommandId() {
        return getEventSource();
    }


}
