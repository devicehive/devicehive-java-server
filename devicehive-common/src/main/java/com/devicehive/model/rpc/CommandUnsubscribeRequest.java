package com.devicehive.model.rpc;

import com.devicehive.shim.api.Body;

import java.util.Date;
import java.util.Objects;
import java.util.Set;

public class CommandUnsubscribeRequest extends Body {

    private String subscriptionId;
    private Set<String> deviceGuids;

    public CommandUnsubscribeRequest(String subscriptionId, Set<String> deviceGuids) {
        super(Action.COMMAND_UNSUBSCRIBE_REQUEST.name());
        this.subscriptionId = subscriptionId;
        this.deviceGuids = deviceGuids;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Set<String> getDeviceGuids() {
        return deviceGuids;
    }

    public void setDeviceGuids(Set<String> deviceGuids) {
        this.deviceGuids = deviceGuids;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommandUnsubscribeRequest)) return false;
        if (!super.equals(o)) return false;

        CommandUnsubscribeRequest that = (CommandUnsubscribeRequest) o;
        return Objects.equals(subscriptionId, that.subscriptionId)
                && Objects.equals(deviceGuids, that.deviceGuids);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), subscriptionId, deviceGuids);
    }

    @Override
    public String toString() {
        return "CommandUnsubscribeRequest{" +
                "subscriptionId='" + subscriptionId + '\'' +
                ", deviceGuids=" + deviceGuids +
                '}';
    }
}
