package com.devicehive.model;


import org.apache.commons.lang3.ObjectUtils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SubscriptionFilterExternal implements HiveEntity {

    private List<DevicesNamesFilter> devices;
    private List<String> names;
    private Timestamp timestamp;

    private SubscriptionFilterExternal() {
    }

    public List<DevicesNamesFilter> getDevices() {
        return devices;
    }

    public void setDevices(List<DevicesNamesFilter> devices) {
        this.devices = devices;
    }

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    public Timestamp getTimestamp() {
        return ObjectUtils.cloneIfPossible(timestamp);
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = ObjectUtils.cloneIfPossible(timestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SubscriptionFilterExternal that = (SubscriptionFilterExternal) o;

        if (devices != null ? !devices.equals(that.devices) : that.devices != null) return false;
        if (names != null ? !names.equals(that.names) : that.names != null) return false;
        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = devices != null ? devices.hashCode() : 0;
        result = 31 * result + (names != null ? names.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        return result;
    }
}
