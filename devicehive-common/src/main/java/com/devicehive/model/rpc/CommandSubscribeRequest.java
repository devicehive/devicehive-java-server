package com.devicehive.model.rpc;

import com.devicehive.shim.api.Body;

import java.util.Date;
import java.util.Objects;
import java.util.Set;

public class CommandSubscribeRequest extends Body {

    private String subscriptionId;
    private String device;
    private Set<String> names;
    private Date timestamp;

    public CommandSubscribeRequest(String subscriptionId, String device, Set<String> names, Date timestamp) {
        super(Action.COMMAND_SUBSCRIBE_REQUEST.name());
        this.subscriptionId = subscriptionId;
        this.device = device;
        this.names = names;
        this.timestamp = timestamp;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public Set<String> getNames() {
        return names;
    }

    public void setNames(Set<String> names) {
        this.names = names;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommandSubscribeRequest)) return false;
        if (!super.equals(o)) return false;
        CommandSubscribeRequest that = (CommandSubscribeRequest) o;
        return Objects.equals(subscriptionId, that.subscriptionId) &&
                Objects.equals(device, that.device) &&
                Objects.equals(names, that.names) &&
                Objects.equals(timestamp, that.timestamp);

    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), subscriptionId, device, names, timestamp);
    }

    @Override
    public String toString() {
        return "CommandSubscribeRequest{" +
                "subscriptionId='" + subscriptionId + '\'' +
                ", device='" + device + '\'' +
                ", names=" + names +
                ", timestamp=" + timestamp +
                '}';
    }
}
