package com.devicehive.model.rpc;

import com.devicehive.shim.api.Action;
import com.devicehive.shim.api.Body;

public abstract class BasePluginRequest extends Body {

    protected Long subscriptionId;

    protected BasePluginRequest(Action action) {
        super(action);
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }
}
