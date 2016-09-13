package com.devicehive.model.rpc;

import com.devicehive.shim.api.Body;

public class CommandGetSubscriptionRequest extends Body {

    private String subscriptionId;

    public CommandGetSubscriptionRequest(String subscriptionId) {
        super(Action.COMMAND_GET_SUBSCRIPTION_REQUEST.name());
        this.subscriptionId = subscriptionId;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }
}
