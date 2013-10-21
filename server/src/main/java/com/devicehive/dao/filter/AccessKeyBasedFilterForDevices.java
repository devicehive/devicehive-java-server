package com.devicehive.dao.filter;

import com.devicehive.model.AccessKeyPermission;

import java.util.*;

public class
        AccessKeyBasedFilterForDevices {

    private Set<String> deviceGuids;

    private Set<Long> networkIds;

    public AccessKeyBasedFilterForDevices(Collection<String> deviceGuids, Collection<Long> networkIds) {
        this.deviceGuids = deviceGuids != null ? new HashSet<>(deviceGuids) : null;
        this.networkIds = networkIds != null ? new HashSet<>(networkIds) : null;
    }

    public Set<String> getDeviceGuids() {
        return deviceGuids == null ? null : Collections.unmodifiableSet(deviceGuids);
    }

    public Set<Long> getNetworkIds() {
        return networkIds == null ? null : Collections.unmodifiableSet(networkIds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AccessKeyBasedFilterForDevices that = (AccessKeyBasedFilterForDevices) o;

        if (deviceGuids != null ? !deviceGuids.equals(that.deviceGuids) : that.deviceGuids != null) return false;
        if (networkIds != null ? !networkIds.equals(that.networkIds) : that.networkIds != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = deviceGuids != null ? deviceGuids.hashCode() : 0;
        result = 31 * result + (networkIds != null ? networkIds.hashCode() : 0);
        return result;
    }

    public static Set<AccessKeyBasedFilterForDevices> createExtraFilters(Set<AccessKeyPermission> permissionSet) {
        if (permissionSet == null) {
            return null;
        }
        Set<AccessKeyBasedFilterForDevices> result = new HashSet<>();
        for (AccessKeyPermission akp : permissionSet) {
            result.add(new AccessKeyBasedFilterForDevices(akp.getDeviceGuidsAsSet(), akp.getNetworkIdsAsSet()));
        }
        return result;
    }
}
