package com.devicehive.client.model;


import org.apache.commons.lang3.ObjectUtils;

import java.sql.Timestamp;
import java.util.Set;

public class SubscriptionFilter {

    private final Set<String> uuids;
    private final Set<String> names;
    private volatile Timestamp timestamp;

    public SubscriptionFilter(Set<String> uuids, Set<String> names, Timestamp timestamp) {
        this.uuids = ObjectUtils.cloneIfPossible(uuids);
        this.names = ObjectUtils.cloneIfPossible(names);
        this.timestamp = ObjectUtils.cloneIfPossible(timestamp);
    }

    public Set<String> getUuids() {
        return ObjectUtils.cloneIfPossible(uuids);
    }

    public Set<String> getNames() {
        return ObjectUtils.cloneIfPossible(names);
    }

    public Timestamp getTimestamp() {
        return ObjectUtils.cloneIfPossible(timestamp);
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = ObjectUtils.cloneIfPossible(timestamp);
    }
}
