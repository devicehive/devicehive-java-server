package com.devicehive.model.rpc;

import com.devicehive.shim.api.Body;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Set;

import static com.devicehive.configuration.Constants.*;

public class SubscribeRequest extends Body {
    private Set<String> devices;
    private Set<String> names;

    public SubscribeRequest(Set<String> devices, Set<String> names) {
        super(Action.NOTIFICATION_SUBSCRIBE_REQUEST.name());
        this.devices = devices;
        this.names = names;
    }

    public Set<String> getDevices() {
        return devices;
    }

    public void setDevices(Set<String> devices) {
        this.devices = devices;
    }

    public Set<String> getNames() {
        return names;
    }

    public void setNames(Set<String> names) {
        this.names = names;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubscribeRequest)) return false;
        if (!super.equals(o)) return false;

        SubscribeRequest that = (SubscribeRequest) o;

        if (devices != null ? !devices.equals(that.devices) : that.devices != null) return false;
        return names != null ? names.equals(that.names) : that.names == null;

    }

    @Override
    public int hashCode() {
        return Objects.hash(action, devices, names);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Body{");
        sb.append("action='").append(action).append("',");
        sb.append(DEVICE_GUIDS).append("=[").append(StringUtils.join(devices, ",")).append("],");
        sb.append(NAMES).append("=[").append(StringUtils.join(names, ",")).append(']');
        sb.append('}');
        return sb.toString();
    }
}
