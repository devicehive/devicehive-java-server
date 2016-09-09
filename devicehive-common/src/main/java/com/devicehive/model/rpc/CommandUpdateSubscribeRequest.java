package com.devicehive.model.rpc;

import com.devicehive.shim.api.Body;

public class CommandUpdateSubscribeRequest extends Body {

    private long commandId;
    private String guid;
    private String subscriptionId;

    public CommandUpdateSubscribeRequest(long commandId, String guid, String subscriptionId) {
        super(Action.COMMAND_UPDATE_SUBSCRIBE_REQUEST.name());
        this.commandId = commandId;
        this.guid = guid;
        this.subscriptionId = subscriptionId;
    }

    public long getCommandId() {
        return commandId;
    }

    public String getGuid() {
        return guid;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }
}
