package com.devicehive.model.rpc;

import com.devicehive.model.eventbus.Subscription;
import com.devicehive.shim.api.Body;

import java.util.Set;

public class CommandGetSubscriptionResponse extends Body {

    private Set<Subscription> subscriptions;

    public CommandGetSubscriptionResponse(Set<Subscription> subscriptions) {
        super(Action.COMMAND_GET_SUBSCRIPTION_RESPONSE.name());
        this.subscriptions = subscriptions;
    }

    public Set<Subscription> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(Set<Subscription> subscriptions) {
        this.subscriptions = subscriptions;
    }
}
