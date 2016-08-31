package com.devicehive.model.rpc;

import com.devicehive.shim.api.Body;

import java.util.Objects;
import java.util.Set;

public class CommandUnsubscribeResponse extends Body {

    private String subId;
    private Set<String> deviceGuids;

    public CommandUnsubscribeResponse(String subId, Set<String> deviceGuids) {
        super(Action.COMMAND_UNSUBSCRIBE_RESPONSE.name());
        this.subId = subId;
        this.deviceGuids = deviceGuids;
    }

    public String getSubId() {
        return subId;
    }

    public void setSubId(String subId) {
        this.subId = subId;
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
        if (!(o instanceof CommandUnsubscribeResponse)) return false;
        if (!super.equals(o)) return false;
        CommandUnsubscribeResponse that = (CommandUnsubscribeResponse) o;
        return Objects.equals(subId, that.subId)
                && Objects.equals(deviceGuids, that.deviceGuids);

    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), subId, deviceGuids);
    }

    @Override
    public String toString() {
        return "CommandUnsubscribeResponse{" +
                "subId='" + subId + '\'' +
                ", deviceGuids=" + deviceGuids +
                '}';
    }
}
