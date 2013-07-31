package com.devicehive.messages.data.cluster.hazelcast.subscriptions.dao;

import static com.devicehive.messages.data.cluster.hazelcast.Constants.COMMAND_UPDATE_SUBSCRIPTION_COMMAND_MAP;
import static com.devicehive.messages.data.cluster.hazelcast.Constants.COMMAND_UPDATE_SUBSCRIPTION_ID;
import static com.devicehive.messages.data.cluster.hazelcast.Constants.COMMAND_UPDATE_SUBSCRIPTION_SESSION_MAP;

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.Dependent;

import com.devicehive.messages.data.subscriptions.model.CommandUpdatesSubscription;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.IMap;

@Dependent
public class CommandUpdatesSubscriptionDAO implements com.devicehive.messages.data.subscriptions.dao.CommandUpdatesSubscriptionDAO {

    private HazelcastInstance hazelcast;

    private IAtomicLong counter;

    /* commandId is unique */
    private IMap<Long, CommandUpdatesSubscription> commandToObject;
    /* sessionId is not unique */
    private IMap<String, Set<CommandUpdatesSubscription>> sessionToObject;

    public CommandUpdatesSubscriptionDAO() {
    }

    @Override
    public synchronized CommandUpdatesSubscription getByCommandId(Long id) {
        CommandUpdatesSubscription entity = commandToObject.get(id);
        return entity;
    }

    @Override
    public synchronized void insert(CommandUpdatesSubscription entity) {
        entity.setId(Long.valueOf(counter.getAndIncrement()));
        commandToObject.put(entity.getCommandId(), entity);

        Set<CommandUpdatesSubscription> records = sessionToObject.get(entity.getSessionId());
        if (records == null) {
            records = new HashSet<>();
        }
        records.add(entity);
        sessionToObject.put(entity.getSessionId(), records);
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

    public void setHazelcast(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
        init();
    }

    protected void init() {
        counter = hazelcast.getAtomicLong(COMMAND_UPDATE_SUBSCRIPTION_ID);
        commandToObject = hazelcast.getMap(COMMAND_UPDATE_SUBSCRIPTION_COMMAND_MAP);
        sessionToObject = hazelcast.getMap(COMMAND_UPDATE_SUBSCRIPTION_SESSION_MAP);
    }
}
