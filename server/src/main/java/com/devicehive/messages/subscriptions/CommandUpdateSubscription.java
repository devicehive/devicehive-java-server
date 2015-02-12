package com.devicehive.messages.subscriptions;


import com.devicehive.messages.handler.HandlerCreator;
import com.devicehive.model.DeviceCommandMessage;

import java.util.UUID;

public class CommandUpdateSubscription extends Subscription<String, DeviceCommandMessage> {

    public CommandUpdateSubscription(String commandId, UUID subscriberId, HandlerCreator<DeviceCommandMessage> handlerCreator) {
        super(commandId, subscriberId, handlerCreator);
    }

    public String getCommandId() {
        return getEventSource();
    }


}
