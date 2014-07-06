package com.devicehive.messages.subscriptions;


import com.devicehive.messages.handler.HandlerCreator;
import com.devicehive.model.DeviceCommand;

import java.util.UUID;

public class CommandUpdateSubscription extends Subscription<Long, DeviceCommand> {

    public CommandUpdateSubscription(Long commandId, UUID subscriberId, HandlerCreator<DeviceCommand> handlerCreator) {
        super(commandId, subscriberId, handlerCreator);
    }

    public Long getCommandId() {
        return getEventSource();
    }


}
