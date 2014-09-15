package com.devicehive.client.impl.context;

import com.devicehive.client.HiveMessageHandler;
import com.devicehive.client.model.HiveMessage;
import com.devicehive.client.model.SubscriptionFilter;

import org.apache.commons.lang3.ObjectUtils;

import java.sql.Timestamp;


public class SubscriptionDescriptor<T extends HiveMessage> {

    private HiveMessageHandler<T> handler;
    private SubscriptionFilter filter;


    public SubscriptionDescriptor(HiveMessageHandler<T> handler, SubscriptionFilter filter) {
        this.handler = handler;
        this.filter = ObjectUtils.cloneIfPossible(filter);
    }

    public void handleMessage(T message) {
        updateTimestamp(message.getTimestamp());
        handler.handle(message);
    }

    public HiveMessageHandler<T> getHandler() {
        return handler;
    }

    public synchronized SubscriptionFilter getFilter() {
        return filter;
    }

    private synchronized void updateTimestamp(Timestamp newTimestamp) {
        if (filter.getTimestamp() == null && newTimestamp != null) {
            filter.setTimestamp(newTimestamp);
        } else if (newTimestamp != null && newTimestamp.after(filter.getTimestamp())) {
            filter.setTimestamp(ObjectUtils.cloneIfPossible(newTimestamp));
        }
    }
}
