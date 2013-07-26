package com.devicehive.messages.data.hash.subscriptions.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import com.devicehive.messages.data.derby.subscriptions.model.NotificationsSubscription;
import com.devicehive.model.Device;

@Stateless
public class NotificationSubscriptionDAO {

    private Lock lock = new ReentrantLock();

    private static long counter = 0L;

    private ConcurrentMap<Key, NotificationsSubscription> keyToObject = new ConcurrentHashMap<>();
    private ConcurrentMap<Long, List<NotificationsSubscription>> deviceToObject = new ConcurrentHashMap<>();
    private ConcurrentMap<String, List<NotificationsSubscription>> sessionToObject = new ConcurrentHashMap<>();

    public void insertSubscriptions(Collection<Long> deviceIds, String sessionId) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            insertSubscriptions((Long) null, sessionId);
        }
        else if (sessionId != null) {
            for (Long deviceId : deviceIds) {
                deleteByDeviceAndSession(deviceId, sessionId);
                insertSubscriptions(deviceId, sessionId);
            }
        }
    }

    public void insertSubscriptions(Long deviceId, String sessionId) {
        if (sessionId != null) {
            try {
                lock.lock();
                NotificationsSubscription entity = new NotificationsSubscription(deviceId, sessionId);
                entity.setId(Long.valueOf(counter));

                Key key = new Key(deviceId, sessionId);
                keyToObject.put(key, entity);

                List<NotificationsSubscription> deviceRecords = deviceToObject.get(deviceId);
                if (deviceRecords == null) {
                    deviceRecords = new ArrayList<>();
                }
                deviceRecords.add(entity);
                deviceToObject.put(deviceId, deviceRecords);

                List<NotificationsSubscription> sessionRecords = sessionToObject.get(sessionId);
                if (sessionRecords == null) {
                    sessionRecords = new ArrayList<>();
                }
                sessionRecords.add(entity);
                sessionToObject.put(sessionId, sessionRecords);

                ++counter;
            }
            finally {
                lock.unlock();
            }
        }
    }

    public void deleteBySession(String sessionId) {
        try {
            lock.lock();
            List<NotificationsSubscription> entities = sessionToObject.remove(sessionId);
            if (entities != null) {
                for (NotificationsSubscription entity : entities) {
                    deviceToObject.remove(entity.getDeviceId());
                    keyToObject.remove(new Key(entity.getDeviceId(), sessionId));
                }
            }

        }
        finally {
            lock.unlock();
        }
    }

    public void deleteByDeviceAndSession(Long deviceId, String sessionId) {
        try {
            lock.lock();
            NotificationsSubscription entity = keyToObject.remove(new Key(deviceId, sessionId));
            deviceToObject.remove(entity.getDeviceId());
        }
        finally {
            lock.unlock();
        }
    }

    public void deleteByDeviceAndSession(Device device, String sessionId) {
        deleteByDeviceAndSession(device.getId(), sessionId);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<String> getSessionIdSubscribedForAll() {
        return getSessionIdSubscribedByDevice(null);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<String> getSessionIdSubscribedByDevice(Long deviceId) {
        List<NotificationsSubscription> records = deviceToObject.get(deviceId);
        if (records != null) {
            List<String> sessions = new ArrayList<>(records.size());
            for (NotificationsSubscription entity : records) {
                sessions.add(entity.getSessionId());
            }
            return sessions;
        }
        return Collections.emptyList();
    }

    private class Key {
        final Long deviceId;
        final String sessionId;

        public Key(Long deviceId, String sessionId) {
            this.deviceId = deviceId;
            this.sessionId = sessionId;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((deviceId == null) ? 0 : deviceId.hashCode());
            result = prime * result + ((sessionId == null) ? 0 : sessionId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Key other = (Key) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (deviceId == null) {
                if (other.deviceId != null)
                    return false;
            }
            else if (!deviceId.equals(other.deviceId))
                return false;
            if (sessionId == null) {
                if (other.sessionId != null)
                    return false;
            }
            else if (!sessionId.equals(other.sessionId))
                return false;
            return true;
        }

        private NotificationSubscriptionDAO getOuterType() {
            return NotificationSubscriptionDAO.this;
        }

    }

}
