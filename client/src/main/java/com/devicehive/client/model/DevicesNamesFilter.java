package com.devicehive.client.model;


import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DevicesNamesFilter implements HiveEntity {
    @SerializedName("deviceGuids")
    private List<String> deviceUuids;

    @SerializedName("names")
    private List<String> names;

    public List<String> getDeviceUuids() {
        return deviceUuids;
    }

    public void setDeviceUuids(List<String> deviceUuids) {
        this.deviceUuids = deviceUuids;
    }

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DevicesNamesFilter that = (DevicesNamesFilter) o;

        if (deviceUuids != null ? !deviceUuids.equals(that.deviceUuids) : that.deviceUuids != null) return false;
        if (names != null ? !names.equals(that.names) : that.names != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = deviceUuids != null ? deviceUuids.hashCode() : 0;
        result = 31 * result + (names != null ? names.hashCode() : 0);
        return result;
    }
}
