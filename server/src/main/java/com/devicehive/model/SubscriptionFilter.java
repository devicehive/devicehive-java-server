package com.devicehive.model;


import org.apache.commons.lang3.ObjectUtils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SubscriptionFilter implements HiveEntity {

    private List<DeviceMessageFilter> deviceFilters;
    private List<String> names;
    private Timestamp timestamp;

    private SubscriptionFilter() {
    }

    public static SubscriptionFilter createForSingleDevice(String deviceUuid, List<String> names, Timestamp timestamp) {
        SubscriptionFilter filter = new SubscriptionFilter();
        filter.setTimestamp(timestamp);
        DeviceMessageFilter deviceMessageFilter = new DeviceMessageFilter();
        deviceMessageFilter.setDeviceUuid(deviceUuid);
        deviceMessageFilter.setNames(names);
        filter.setDeviceFilters(Arrays.asList(deviceMessageFilter));
        return filter;
    }

    public static SubscriptionFilter createForAllDevices(List<String> names, Timestamp timestamp) {
        SubscriptionFilter filter = new SubscriptionFilter();
        filter.setTimestamp(timestamp);
        filter.setNames(names);
        return filter;
    }

    public static SubscriptionFilter createForManyDevices(List<String> deviceUuids, Timestamp timestamp) {
        SubscriptionFilter filter = new SubscriptionFilter();
        filter.setTimestamp(timestamp);
        if (deviceUuids != null) {
            List<DeviceMessageFilter> dmfs = new ArrayList<>(deviceUuids.size());
            for (String deviceUuid : deviceUuids) {
                DeviceMessageFilter dmf = new DeviceMessageFilter();
                dmf.setDeviceUuid(deviceUuid);
                dmfs.add(dmf);
            }
            filter.setDeviceFilters(dmfs);
        }
        return filter;
    }

    public List<DeviceMessageFilter> getDeviceFilters() {
        return deviceFilters;
    }

    public void setDeviceFilters(List<DeviceMessageFilter> deviceFilters) {
        this.deviceFilters = deviceFilters;
    }

    public Timestamp getTimestamp() {
        return ObjectUtils.cloneIfPossible(timestamp);
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = ObjectUtils.cloneIfPossible(timestamp);
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

        SubscriptionFilter that = (SubscriptionFilter) o;

        if (deviceFilters != null ? !deviceFilters.equals(that.deviceFilters) : that.deviceFilters != null)
            return false;
        if (names != null ? !names.equals(that.names) : that.names != null) return false;
        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = deviceFilters != null ? deviceFilters.hashCode() : 0;
        result = 31 * result + (names != null ? names.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        return result;
    }
}
