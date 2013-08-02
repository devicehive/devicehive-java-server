package com.devicehive.messages.data.subscriptions.dao;

import com.devicehive.messages.data.subscriptions.model.CommandsSubscription;

public interface CommandSubscriptionDAO {

    //@TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public CommandsSubscription getByDeviceId(Long id);

    public void insert(CommandsSubscription subscription);

    public void deleteBySession(String sessionId);

    public void deleteByDevice(Long deviceId);
}
