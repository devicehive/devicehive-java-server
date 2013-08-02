package com.devicehive.messages.data.subscriptions.dao;

import com.devicehive.messages.data.subscriptions.model.CommandUpdatesSubscription;

public interface CommandUpdatesSubscriptionDAO {

    //@TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public CommandUpdatesSubscription getByCommandId(Long id);

    public void insert(CommandUpdatesSubscription subscription);

    public void deleteBySession(String sessionId);

    public void deleteByCommandId(Long commandId);
}
