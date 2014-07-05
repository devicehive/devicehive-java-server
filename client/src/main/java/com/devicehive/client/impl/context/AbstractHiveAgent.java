package com.devicehive.client.impl.context;


import com.devicehive.client.impl.Status;
import com.devicehive.client.impl.util.LockWrapper;
import com.devicehive.client.impl.util.Messages;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.exceptions.HiveException;
import com.google.common.base.Preconditions;

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
        try ( LockWrapper lw = LockWrapper.read(stateLock)) {
            return hivePrincipal;
        }
    }

    protected abstract void beforeConnect()throws HiveException;

    protected abstract void doConnect() throws HiveException;

    protected abstract void afterConnect() throws HiveException;

    protected abstract void beforeDisconnect();

    protected abstract void doDisconnect();

    protected abstract void afterDisconnect();


    public final void connect() throws HiveException {
        try ( LockWrapper lw = LockWrapper.write(stateLock)) {
            beforeConnect();
            doConnect();
            afterConnect();
        }
    }

    public final void disconnect() {
        try ( LockWrapper lw = LockWrapper.write(stateLock)) {
            beforeDisconnect();
            doDisconnect();
            afterDisconnect();
        }
    }


    public void authenticate(HivePrincipal hivePrincipal) throws HiveException {
        try ( LockWrapper lw = LockWrapper.write(stateLock)) {
            if (this.hivePrincipal != null && !this.hivePrincipal.equals(hivePrincipal)) {
                throw new IllegalStateException(Messages.ALREADY_AUTHENTICATED);
            }
            this.hivePrincipal = hivePrincipal;
        }
    }


    public final void close()  {
        try ( LockWrapper lw = LockWrapper.write(stateLock)) {
            disconnect();
        }
    }
}
