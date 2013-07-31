package com.devicehive.messages.data.cluster.hazelcast.subscriptions.dao;

import static com.devicehive.messages.data.cluster.hazelcast.Constants.COMMAND_SUBSCRIPTION_DEVICE_MAP;
import static com.devicehive.messages.data.cluster.hazelcast.Constants.COMMAND_SUBSCRIPTION_ID;
import static com.devicehive.messages.data.cluster.hazelcast.Constants.COMMAND_SUBSCRIPTION_SESSION_MAP;

import javax.enterprise.context.Dependent;

import com.devicehive.messages.data.subscriptions.model.CommandsSubscription;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.IMap;

@Dependent
public class CommandSubscriptionDAO implements com.devicehive.messages.data.subscriptions.dao.CommandSubscriptionDAO {

    private HazelcastInstance hazelcast;

    private IAtomicLong counter;

    /* 1 device per 1 session, no more no less */
    /* deviceId is unique */
    private IMap<Long, CommandsSubscription> deviceToObject;
    /* sessionId is not unique */
    private IMap<String, CommandsSubscription> sessionToObject;

    public CommandSubscriptionDAO() {
    }

    @Override
    public CommandsSubscription getByDeviceId(Long id) {
        CommandsSubscription entity = deviceToObject.get(id);
        return entity;
    }

    @Override
    public synchronized void insert(CommandsSubscription entity) {
        entity.setId(Long.valueOf(counter.getAndIncrement()));
        deviceToObject.put(entity.getDeviceId(), entity);
        sessionToObject.put(entity.getSessionId(), entity);
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

    public void setHazelcast(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
        init();
    }

    protected void init() {
        counter = hazelcast.getAtomicLong(COMMAND_SUBSCRIPTION_ID);
        deviceToObject = hazelcast.getMap(COMMAND_SUBSCRIPTION_DEVICE_MAP);
        sessionToObject = hazelcast.getMap(COMMAND_SUBSCRIPTION_SESSION_MAP);
    }

}
