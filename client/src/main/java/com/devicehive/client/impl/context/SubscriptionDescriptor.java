package com.devicehive.client.impl.context;

import com.devicehive.client.HiveMessageHandler;
import com.devicehive.client.model.SubscriptionFilter;
import org.apache.commons.lang3.ObjectUtils;

import java.sql.Timestamp;


public class SubscriptionDescriptor<T> {

    private HiveMessageHandler<T> handler;
    private SubscriptionFilter filter;


    public SubscriptionDescriptor(HiveMessageHandler<T> handler, SubscriptionFilter filter) {
        this.handler = handler;
        this.filter = filter;
    }

    public synchronized HiveMessageHandler<T> getHandler() {
        return handler;
    }

    public synchronized SubscriptionFilter getFilter() {
        return filter;
    }

    public synchronized void updateTimestamp(Timestamp newTimestamp) {
        if (filter.getTimestamp() == null && newTimestamp != null) {
            filter.setTimestamp(newTimestamp);
        } else if (newTimestamp != null && newTimestamp.after(filter.getTimestamp())) {
            filter.setTimestamp(ObjectUtils.cloneIfPossible(newTimestamp));
        }
    }
}
