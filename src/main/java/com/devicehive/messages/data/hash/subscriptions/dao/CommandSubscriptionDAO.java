package com.devicehive.messages.data.hash.subscriptions.dao;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.ejb.Stateless;

import com.devicehive.messages.data.derby.subscriptions.model.CommandsSubscription;

@Stateless
public class CommandSubscriptionDAO {

    private Lock lock = new ReentrantLock();

    private static long counter = 0L;

    private ConcurrentMap<Long, CommandsSubscription> deviceToObject = new ConcurrentHashMap<>();
    private ConcurrentMap<String, CommandsSubscription> sessionToObject = new ConcurrentHashMap<>();

    public CommandsSubscription getByDeviceId(Long id) {
        CommandsSubscription entity = deviceToObject.get(id);
        return entity;
    }

    public void insert(CommandsSubscription entity) {
        try {
            lock.lock();
            entity.setId(Long.valueOf(counter));
            deviceToObject.put(entity.getDeviceId(), entity);
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
            CommandsSubscription entity = sessionToObject.remove(sessionId);
            if (entity != null) {
                deviceToObject.remove(entity.getDeviceId());
            }
        }
        finally {
            lock.unlock();
        }
    }

    public void deleteByDevice(Long deviceId) {
        try {
            lock.lock();
            CommandsSubscription entity = deviceToObject.remove(deviceId);
            if (entity != null) {
                sessionToObject.remove(entity.getSessionId());
            }
        }
        finally {
            lock.unlock();
        }
    }

}
