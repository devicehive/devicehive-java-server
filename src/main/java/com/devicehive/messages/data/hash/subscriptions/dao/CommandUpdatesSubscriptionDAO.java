package com.devicehive.messages.data.hash.subscriptions.dao;

import java.util.HashMap;
import java.util.Map;

import com.devicehive.messages.data.derby.subscriptions.model.CommandUpdatesSubscription;

//@Stateless
public class CommandUpdatesSubscriptionDAO {

    private static long counter = 0L;

    private Map<Long, CommandUpdatesSubscription> commandToObject = new HashMap<>();
    private Map<String, CommandUpdatesSubscription> sessionToObject = new HashMap<>();

    public synchronized CommandUpdatesSubscription getByCommandId(Long id) {
        CommandUpdatesSubscription entity = commandToObject.get(id);
        return entity;
    }

    public synchronized void insert(CommandUpdatesSubscription entity) {
        entity.setId(Long.valueOf(counter));
        commandToObject.put(entity.getCommandId(), entity);
        sessionToObject.put(entity.getSessionId(), entity);
        ++counter;
    }

    public synchronized void deleteBySession(String sessionId) {
        CommandUpdatesSubscription entity = sessionToObject.remove(sessionId);
        if (entity != null) {
            commandToObject.remove(entity.getCommandId());
        }
    }

    public synchronized void deleteByCommandId(Long commandId) {
        CommandUpdatesSubscription entity = commandToObject.remove(commandId);
        if (entity != null) {
            sessionToObject.remove(entity.getSessionId());
        }
    }

}
