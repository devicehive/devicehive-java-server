package com.devicehive.client.impl.context;


import org.apache.commons.lang3.ObjectUtils;

import java.sql.Timestamp;
import java.util.*;

public class Subscription {

    private Set<String> names = new HashSet<>();
    private List<String> deviceIds = new LinkedList<>();
    private Timestamp lastTimestamp;

    public Subscription(Timestamp lastTimestamp, Set<String> names, String... ids) {
        this.lastTimestamp = ObjectUtils.cloneIfPossible(lastTimestamp);
        if (names != null)
            this.names.addAll(names);
        if (ids != null)
            Collections.addAll(deviceIds, ids);
    }

    public Timestamp getLastTimestamp() {
        return ObjectUtils.cloneIfPossible(lastTimestamp);
    }

    public Set<String> getNames() {
        return names;
    }

    public List<String> getDeviceIds() {
        return deviceIds;
    }

    public void setDeviceIds(String... deviceIds) {
        this.deviceIds.clear();
        if (deviceIds != null)
            Collections.addAll(this.deviceIds, deviceIds);
    }

    public void setSubscription(Timestamp lastTimestamp, Set<String> names, String... ids){
        this.lastTimestamp = ObjectUtils.cloneIfPossible(lastTimestamp);
        this.names = names;
        setDeviceIds(ids);
    }

}
