package com.devicehive.client.impl.context;

import com.devicehive.client.HiveMessageHandler;
import com.devicehive.client.model.SubscriptionFilter;
import org.apache.commons.lang3.ObjectUtils;

import java.sql.Timestamp;

public abstract class SubscriptionHandler<T> implements HiveMessageHandler<T> {

    private final SubscriptionFilter originalFilter;
    private Timestamp recentTimestamp;


    public SubscriptionHandler(SubscriptionFilter filter) {
        this.originalFilter = ObjectUtils.cloneIfPossible(filter);
    }

    protected synchronized void updateRecentTimestamp(Timestamp timestamp) {
        if (recentTimestamp == null) {
            recentTimestamp = timestamp;
        } else if (recentTimestamp.before(timestamp)) {
            recentTimestamp = timestamp;
        }
    }

    public synchronized SubscriptionFilter getRecentSubscriptionFilter() {
        SubscriptionFilter clone = ObjectUtils.cloneIfPossible(originalFilter);
        clone.setTimestamp(recentTimestamp);
        return clone;
    }

}
