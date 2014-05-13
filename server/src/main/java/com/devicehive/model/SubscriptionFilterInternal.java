package com.devicehive.model;


import com.devicehive.exceptions.HiveException;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.ObjectUtils;

import java.sql.Timestamp;
import java.util.*;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

public class SubscriptionFilterInternal {

    private Map<String, Set<String>> deviceNames;
    private Set<String> names;
    private Timestamp timestamp;

    private SubscriptionFilterInternal() {
    }

    public static SubscriptionFilterInternal createForSingleDevice(String deviceUuid, List<String> names, Timestamp timestamp) {
        SubscriptionFilterInternal filter = new SubscriptionFilterInternal();
        filter.setTimestamp(timestamp);
        if (deviceUuid == null) {
            throw new HiveException("device can not be empty", BAD_REQUEST.getStatusCode());
        }
        Map<String, Set<String>> deviceNames = new HashMap<>();
        deviceNames.put(deviceUuid, names != null ? new HashSet<String>(names): null);
        filter.setDeviceNames(deviceNames);
        return filter;
    }

    public static SubscriptionFilterInternal createForAllDevices(List<String> names, Timestamp timestamp) {
        SubscriptionFilterInternal filter = new SubscriptionFilterInternal();
        filter.setTimestamp(timestamp);
        filter.setNames(ImmutableSet.copyOf(names));
        return filter;
    }

    public static SubscriptionFilterInternal createForManyDevices(List<String> deviceUuids, Timestamp timestamp) {
        SubscriptionFilterInternal filter = new SubscriptionFilterInternal();
        filter.setTimestamp(timestamp);
        if (deviceUuids != null) {
            Map<String, Set<String>> deviceNames = new HashMap<>();
            for (String deviceUuid : deviceUuids) {
                deviceNames.put(deviceUuid, null);
            }
            filter.setDeviceNames(deviceNames);
        }
        return filter;
    }

    public static SubscriptionFilterInternal create(SubscriptionFilterExternal external) {
        SubscriptionFilterInternal filter = new SubscriptionFilterInternal();
        filter.setTimestamp(external.getTimestamp());
        if (external.getNames() != null) {
            filter.setNames(ImmutableSet.copyOf(external.getNames()));
        }
        if (external.getDevices() != null) {
            Map<String, Set<String>> deviceNamesFilters = Maps.newHashMap();
            Set<String> devicesWithoutNameFilters = Sets.newHashSet();
            for (DevicesNamesFilter devicesNamesFilter : external.getDevices()) {
                if (devicesNamesFilter.getDeviceUuids() == null) {
                    throw new HiveException("device can not be empty", BAD_REQUEST.getStatusCode());
                }
                for (String deviceUuid : devicesNamesFilter.getDeviceUuids()) {
                    if (deviceUuid == null) {
                        throw new HiveException("device can not be empty", BAD_REQUEST.getStatusCode());
                    }
                    List<String> newNamesForDevice = devicesNamesFilter.getNames();
                    if (newNamesForDevice == null) {
                        devicesWithoutNameFilters.add(deviceUuid);
                    } else {
                        Set<String> namesForDevice = deviceNamesFilters.get(deviceUuid);
                        if (namesForDevice == null) {
                            namesForDevice = Sets.newHashSet(newNamesForDevice);
                        } else {
                            namesForDevice.addAll(newNamesForDevice);
                        }
                        deviceNamesFilters.put(deviceUuid, namesForDevice);
                    }
                }
                for (String deviceUuid : devicesWithoutNameFilters) {
                    deviceNamesFilters.put(deviceUuid, null);
                }
                filter.setDeviceNames(deviceNamesFilters);
            }
        }
        return filter;
    }

    public Map<String, Set<String>> getDeviceNames() {
        return deviceNames;
    }

    public void setDeviceNames(Map<String, Set<String>> deviceNames) {
        this.deviceNames = deviceNames;
    }

    public Timestamp getTimestamp() {
        return ObjectUtils.cloneIfPossible(timestamp);
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = ObjectUtils.cloneIfPossible(timestamp);
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
        if (o == null || getClass() != o.getClass()) return false;

        SubscriptionFilterInternal that = (SubscriptionFilterInternal) o;

        if (deviceNames != null ? !deviceNames.equals(that.deviceNames) : that.deviceNames != null) return false;
        if (names != null ? !names.equals(that.names) : that.names != null) return false;
//        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = deviceNames != null ? deviceNames.hashCode() : 0;
        result = 31 * result + (names != null ? names.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        return result;
    }
}
