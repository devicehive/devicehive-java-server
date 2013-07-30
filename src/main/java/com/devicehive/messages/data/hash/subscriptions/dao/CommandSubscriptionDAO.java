package com.devicehive.messages.data.hash.subscriptions.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.Dependent;

import com.devicehive.messages.data.subscriptions.model.CommandsSubscription;

@Dependent
public class CommandSubscriptionDAO implements com.devicehive.messages.data.subscriptions.dao.CommandSubscriptionDAO {

    private long counter = 0L;

    /* deviceId is unique */
    private Map<Long, CommandsSubscription> deviceToObject = new HashMap<>();
    /* sessionId is not unique */
    private Map<String, Set<CommandsSubscription>> sessionToObject = new HashMap<>();

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

        Set<CommandsSubscription> records = sessionToObject.get(entity.getSessionId());
        if (records == null) {
            records = new HashSet<>();
        }
        records.add(entity);
        sessionToObject.put(entity.getSessionId(), records);

        ++counter;
    }

    @Override
    public synchronized void deleteBySession(String sessionId) {
        Set<CommandsSubscription> records = sessionToObject.remove(sessionId);
        if (records != null) {
            for (CommandsSubscription entity : records) {
                deviceToObject.remove(entity.getDeviceId());
            }

        }
    }

    @Override
    public synchronized void deleteByDevice(Long deviceId) {
        CommandsSubscription entity = deviceToObject.remove(deviceId);
        if (entity != null) {
            Set<CommandsSubscription> records = sessionToObject.get(entity.getSessionId());
            if (records != null) {
                records.remove(entity);
            }
        }
    }

}
