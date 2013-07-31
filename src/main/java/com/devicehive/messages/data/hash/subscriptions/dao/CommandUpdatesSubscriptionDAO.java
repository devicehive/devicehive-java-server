package com.devicehive.messages.data.hash.subscriptions.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.Dependent;

import com.devicehive.messages.data.subscriptions.model.CommandUpdatesSubscription;

@Dependent
public class CommandUpdatesSubscriptionDAO implements com.devicehive.messages.data.subscriptions.dao.CommandUpdatesSubscriptionDAO {

    private long counter = 0L;

    /* commandId is unique */
    private Map<Long, CommandUpdatesSubscription> commandToObject = new HashMap<>();
    /* sessionId is not unique */
    private Map<String, Set<CommandUpdatesSubscription>> sessionToObject = new HashMap<>();

    public CommandUpdatesSubscriptionDAO() {
    }

    @Override
    public synchronized CommandUpdatesSubscription getByCommandId(Long id) {
        CommandUpdatesSubscription entity = commandToObject.get(id);
        return entity;
    }

    @Override
    public synchronized void insert(CommandUpdatesSubscription entity) {
        entity.setId(Long.valueOf(counter));
        commandToObject.put(entity.getCommandId(), entity);

        Set<CommandUpdatesSubscription> records = sessionToObject.get(entity.getSessionId());
        if (records == null) {
            records = new HashSet<>();
        }
        records.add(entity);
        sessionToObject.put(entity.getSessionId(), records);
        ++counter;
    }

    @Override
    public synchronized void deleteBySession(String sessionId) {
        Set<CommandUpdatesSubscription> records = sessionToObject.remove(sessionId);
        if (records != null) {
            for (CommandUpdatesSubscription entity : records) {
                commandToObject.remove(entity.getCommandId());
            }
        }
    }

    @Override
    public synchronized void deleteByCommandId(Long commandId) {
        CommandUpdatesSubscription entity = commandToObject.remove(commandId);
        if (entity != null) {
            Set<CommandUpdatesSubscription> records = sessionToObject.get(entity.getSessionId());
            if (records != null) {
                records.remove(entity);
            }
        }
    }

}
