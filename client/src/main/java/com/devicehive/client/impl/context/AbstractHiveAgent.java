package com.devicehive.client.impl.context;


import com.devicehive.client.impl.util.Messages;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.exceptions.HiveException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class AbstractHiveAgent {

    private final ConcurrentMap<String, SubscriptionDescriptor<DeviceCommand>> commandSubscriptionsStorage =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<String, SubscriptionDescriptor<DeviceNotification>> notificationSubscriptionsStorage =
            new ConcurrentHashMap<>();

    protected final ReadWriteLock stateLock = new ReentrantReadWriteLock(true);
    private HivePrincipal hivePrincipal;

    protected final ConcurrentMap<String, SubscriptionDescriptor<DeviceCommand>> getCommandSubscriptionsStorage() {
        return commandSubscriptionsStorage;
    }

    protected final ConcurrentMap<String, SubscriptionDescriptor<DeviceNotification>> getNotificationSubscriptionsStorage() {
        return notificationSubscriptionsStorage;
    }


    protected final SubscriptionDescriptor<DeviceCommand> getCommandsSubscriptionDescriptor(String subscriptionId) {
        return commandSubscriptionsStorage.get(subscriptionId);
    }

    protected final SubscriptionDescriptor<DeviceNotification> getNotificationsSubscriptionDescriptor(String subscriptionId) {
        return notificationSubscriptionsStorage.get(subscriptionId);
    }

    protected final void addCommandsSubscription(String newSubscriptionId,
                                                 SubscriptionDescriptor<DeviceCommand> descriptor) {
        commandSubscriptionsStorage.put(newSubscriptionId, descriptor);
    }



    protected final void addNotificationsSubscription(String subscriptionId,
                                                      SubscriptionDescriptor<DeviceNotification> descriptor) {
        notificationSubscriptionsStorage.put(subscriptionId, descriptor);
    }

    protected final void removeCommandsSubscription(String subscriptionId) throws HiveException {
        commandSubscriptionsStorage.remove(subscriptionId);
    }

    protected final void removeNotificationsSubscription(String subscriptionId) throws HiveException {
        notificationSubscriptionsStorage.remove(subscriptionId);
    }

    public HivePrincipal getHivePrincipal() {
        stateLock.readLock().lock();
        try {
            return hivePrincipal;
        } finally {
            stateLock.readLock().unlock();
        }
    }

    protected abstract void beforeConnect()throws HiveException;

    protected abstract void doConnect() throws HiveException;

    protected abstract void afterConnect() throws HiveException;

    protected abstract void beforeDisconnect();

    protected abstract void doDisconnect();

    protected abstract void afterDisconnect();


    public final void connect() throws HiveException {
        stateLock.writeLock().lock();
        try {
            beforeConnect();
            doConnect();
            afterConnect();
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    public final void disconnect() {
        stateLock.writeLock().lock();
        try {
            beforeDisconnect();
            doDisconnect();
            afterDisconnect();
        } finally {
            stateLock.writeLock().unlock();
        }
    }


    public void authenticate(HivePrincipal hivePrincipal) throws HiveException {
        stateLock.writeLock().lock();
        try {
            if (this.hivePrincipal != null && !this.hivePrincipal.equals(hivePrincipal)) {
                throw new IllegalStateException(Messages.ALREADY_AUTHENTICATED);
            }
            this.hivePrincipal = hivePrincipal;
        } finally {
            stateLock.writeLock().unlock();
        }
    }


    public final void close()  {
        stateLock.writeLock().lock();
        try {
            disconnect();
        } finally {
            stateLock.writeLock().unlock();
        }
    }
}
