package com.devicehive.model;


import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DeviceMessageFilter implements HiveEntity {

    @SerializedName("deviceGuid")
    private String deviceUuid;

    @SerializedName("names")
    private List<String> names;


    public DeviceMessageFilter() {
    }

    public DeviceMessageFilter(String deviceUuid, List<String> names) {
        this.deviceUuid = deviceUuid;
        this.names = names;
    }

    public String getDeviceUuid() {
        return deviceUuid;
    }

    public void setDeviceUuid(String deviceUuid) {
        this.deviceUuid = deviceUuid;
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

        DeviceMessageFilter that = (DeviceMessageFilter) o;

        if (deviceUuid != null ? !deviceUuid.equals(that.deviceUuid) : that.deviceUuid != null) return false;
        if (names != null ? !names.equals(that.names) : that.names != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = deviceUuid != null ? deviceUuid.hashCode() : 0;
        result = 31 * result + (names != null ? names.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DeviceSubDescriptor{" +
                "deviceUuid='" + deviceUuid + '\'' +
                ", names=" + names +
                '}';
    }
}
