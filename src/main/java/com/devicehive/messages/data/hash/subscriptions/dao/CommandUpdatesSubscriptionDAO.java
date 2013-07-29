package com.devicehive.messages.data.hash.subscriptions.dao;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.Dependent;

import com.devicehive.messages.data.subscriptions.model.CommandUpdatesSubscription;

@Dependent
public class CommandUpdatesSubscriptionDAO implements com.devicehive.messages.data.subscriptions.dao.CommandUpdatesSubscriptionDAO {

    private long counter = 0L;

    private Map<Long, CommandUpdatesSubscription> commandToObject = new HashMap<>();
    private Map<String, CommandUpdatesSubscription> sessionToObject = new HashMap<>();
    
    public CommandUpdatesSubscriptionDAO() {}

    @Override
    public synchronized CommandUpdatesSubscription getByCommandId(Long id) {
        CommandUpdatesSubscription entity = commandToObject.get(id);
        return entity;
    }

    @Override
    public synchronized void insert(CommandUpdatesSubscription entity) {
        entity.setId(Long.valueOf(counter));
        commandToObject.put(entity.getCommandId(), entity);
        sessionToObject.put(entity.getSessionId(), entity);
        ++counter;
    }

    @Override
    public synchronized void deleteBySession(String sessionId) {
        CommandUpdatesSubscription entity = sessionToObject.remove(sessionId);
        if (entity != null) {
            commandToObject.remove(entity.getCommandId());
        }
    }

    @Override
    public synchronized void deleteByCommandId(Long commandId) {
        CommandUpdatesSubscription entity = commandToObject.remove(commandId);
        if (entity != null) {
            sessionToObject.remove(entity.getSessionId());
        }
    }

}
