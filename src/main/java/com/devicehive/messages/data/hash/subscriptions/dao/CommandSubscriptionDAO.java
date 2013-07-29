package com.devicehive.messages.data.hash.subscriptions.dao;

import java.util.HashMap;
import java.util.Map;

import com.devicehive.messages.data.derby.subscriptions.model.CommandsSubscription;

//@Stateless
public class CommandSubscriptionDAO {

    private static long counter = 0L;

    private Map<Long, CommandsSubscription> deviceToObject = new HashMap<>();
    private Map<String, CommandsSubscription> sessionToObject = new HashMap<>();

    public CommandsSubscription getByDeviceId(Long id) {
        CommandsSubscription entity = deviceToObject.get(id);
        return entity;
    }

    public synchronized void insert(CommandsSubscription entity) {
        entity.setId(Long.valueOf(counter));
        deviceToObject.put(entity.getDeviceId(), entity);
        sessionToObject.put(entity.getSessionId(), entity);
        ++counter;
    }

    public synchronized void deleteBySession(String sessionId) {
        CommandsSubscription entity = sessionToObject.remove(sessionId);
        if (entity != null) {
            deviceToObject.remove(entity.getDeviceId());
        }
    }

    public synchronized void deleteByDevice(Long deviceId) {
        CommandsSubscription entity = deviceToObject.remove(deviceId);
        if (entity != null) {
            sessionToObject.remove(entity.getSessionId());
        }
    }

}
