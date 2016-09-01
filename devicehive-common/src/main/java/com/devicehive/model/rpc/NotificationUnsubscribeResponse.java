package com.devicehive.model.rpc;

import com.devicehive.shim.api.Body;

import java.util.Objects;
import java.util.Set;

public class NotificationUnsubscribeResponse extends Body {

    private String subscriptionId;
    private Set<String> deviceGuids;

    public NotificationUnsubscribeResponse(String subscriptionId, Set<String> deviceGuids) {
        super(Action.NOTIFICATION_UNSUBSCRIBE_RESPONSE.name());
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
        if (!(o instanceof NotificationUnsubscribeResponse)) return false;
        if (!super.equals(o)) return false;
        NotificationUnsubscribeResponse that = (NotificationUnsubscribeResponse) o;
        return Objects.equals(subscriptionId, that.subscriptionId)
                && Objects.equals(deviceGuids, that.deviceGuids);

    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), subscriptionId, deviceGuids);
    }

    @Override
    public String toString() {
        return "NotificationUnsubscribeResponse{" +
                "subscriptionId='" + subscriptionId + '\'' +
                ", deviceGuids=" + deviceGuids +
                '}';
    }

}
