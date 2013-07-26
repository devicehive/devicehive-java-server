package com.devicehive.messages.data.hash.subscriptions.dao;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.ejb.Stateless;

import com.devicehive.messages.data.derby.subscriptions.model.CommandUpdatesSubscription;

@Stateless
public class CommandUpdatesSubscriptionDAO {

    private Lock lock = new ReentrantLock();

    private static long counter = 0L;

    private ConcurrentMap<Long, CommandUpdatesSubscription> commandToObject = new ConcurrentHashMap<>();
    private ConcurrentMap<String, CommandUpdatesSubscription> sessionToObject = new ConcurrentHashMap<>();

    public CommandUpdatesSubscription getByCommandId(Long id) {
        CommandUpdatesSubscription entity = commandToObject.get(id);
        return entity;
    }

    public void insert(CommandUpdatesSubscription entity) {
        try {
            lock.lock();
            entity.setId(Long.valueOf(counter));
            commandToObject.put(entity.getCommandId(), entity);
            sessionToObject.put(entity.getSessionId(), entity);
            ++counter;
        }
        finally {
            lock.unlock();
        }
    }

    public void deleteBySession(String sessionId) {
        try {
            lock.lock();
            CommandUpdatesSubscription entity = sessionToObject.remove(sessionId);
            if (entity != null) {
                commandToObject.remove(entity.getCommandId());
            }
        }
        finally {
            lock.unlock();
        }
    }

    public void deleteByCommandId(Long commandId) {
        try {
            lock.lock();
            CommandUpdatesSubscription entity = commandToObject.remove(commandId);
            if (entity != null) {
                sessionToObject.remove(entity.getSessionId());
            }
        }
        finally {
            lock.unlock();
        }
    }

}
