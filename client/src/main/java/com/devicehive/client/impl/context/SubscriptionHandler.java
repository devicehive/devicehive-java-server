package com.devicehive.client.impl.context;

import com.devicehive.client.MessageHandler;
import com.devicehive.client.model.SubscriptionFilter;
import org.apache.commons.lang3.ObjectUtils;

import java.sql.Timestamp;

public abstract class SubscriptionHandler<T> implements MessageHandler<T> {

    private final SubscriptionFilter originalFilter;
    private final MessageHandler<T> handler;
    private Timestamp recentTimestamp;


    public SubscriptionHandler(SubscriptionFilter filter, MessageHandler<T> handler) {
        this.originalFilter = ObjectUtils.cloneIfPossible(filter);
        this.handler = handler;
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
