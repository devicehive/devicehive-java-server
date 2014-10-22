package com.devicehive.client.impl.context;

import com.devicehive.client.HiveMessageHandler;
import com.devicehive.client.model.HiveMessage;
import com.devicehive.client.model.SubscriptionFilter;

import org.apache.commons.lang3.ObjectUtils;

import java.sql.Timestamp;

public class SubscriptionDescriptor<T extends HiveMessage> {

    private final HiveMessageHandler<T> handler;
    private final SubscriptionFilter filter;

    public SubscriptionDescriptor(final HiveMessageHandler<T> handler, final SubscriptionFilter filter) {
        this.handler = handler;
        this.filter = ObjectUtils.cloneIfPossible(filter);
    }

    public void handleMessage(final T message) {
        updateTimestamp(message.getTimestamp());
        handler.handle(message);
    }

    public SubscriptionFilter getFilter() {
        return filter;
    }

    private void updateTimestamp(final Timestamp newTimestamp) {
        if (filter.getTimestamp() == null && newTimestamp != null) {
            filter.setTimestamp(newTimestamp);
        } else if (newTimestamp != null && newTimestamp.after(filter.getTimestamp())) {
            filter.setTimestamp(newTimestamp);
        }
    }
}
