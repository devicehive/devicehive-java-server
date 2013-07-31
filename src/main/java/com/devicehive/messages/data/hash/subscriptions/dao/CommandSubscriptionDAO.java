package com.devicehive.messages.data.hash.subscriptions.dao;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.Dependent;

import com.devicehive.messages.data.subscriptions.model.CommandsSubscription;

@Dependent
public class CommandSubscriptionDAO implements com.devicehive.messages.data.subscriptions.dao.CommandSubscriptionDAO {

    private long counter = 0L;

    /* 1 device per 1 session, no more no less */
    /* deviceId is unique */
    private Map<Long, CommandsSubscription> deviceToObject = new HashMap<>();
    /* sessionId is not unique */
    private Map<String, CommandsSubscription> sessionToObject = new HashMap<>();

    public CommandSubscriptionDAO() {
    }

    @Override
    public CommandsSubscription getByDeviceId(Long id) {
        CommandsSubscription entity = deviceToObject.get(id);
        return entity;
    }

    @Override
    public synchronized void insert(CommandsSubscription entity) {
        entity.setId(Long.valueOf(counter));
        deviceToObject.put(entity.getDeviceId(), entity);
        sessionToObject.put(entity.getSessionId(), entity);
        ++counter;
    }

    @Override
    public synchronized void deleteBySession(String sessionId) {
        /* Remove session - removes device, device must reconnect if no session */
        CommandsSubscription entity = sessionToObject.remove(sessionId);
        if (entity != null) {
            deviceToObject.remove(entity.getDeviceId());
        }
    }

    @Override
    public synchronized void deleteByDevice(Long deviceId) {
        /* Remove device - removes session. No Session if no device */
        CommandsSubscription entity = deviceToObject.remove(deviceId);
        if (entity != null) {
            sessionToObject.remove(entity.getSessionId());
        }
    }

}
