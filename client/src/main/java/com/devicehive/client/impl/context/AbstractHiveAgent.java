package com.devicehive.client.impl.context;


import com.devicehive.client.ConnectionLostCallback;
import com.devicehive.client.ConnectionRestoredCallback;
import com.devicehive.client.impl.util.Messages;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.exceptions.HiveException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class AbstractHiveAgent {

    protected final ConcurrentMap<String, SubscriptionDescriptor<DeviceCommand>> commandSubscriptionsStorage =
            new ConcurrentHashMap<>();
    protected final ConcurrentMap<String, SubscriptionDescriptor<DeviceNotification>> notificationSubscriptionsStorage =
            new ConcurrentHashMap<>();

    private HivePrincipal hivePrincipal;

    protected final ConnectionLostCallback connectionLostCallback;

    protected final ConnectionRestoredCallback connectionRestoredCallback;

    protected final ExecutorService connectionStateExecutor = Executors.newSingleThreadExecutor();


    protected final ReadWriteLock connectionLock = new ReentrantReadWriteLock(true);
    protected final ReadWriteLock subscriptionsLock = new ReentrantReadWriteLock(true);


    protected AbstractHiveAgent(ConnectionLostCallback connectionLostCallback, ConnectionRestoredCallback connectionRestoredCallback) {
        this.connectionLostCallback = connectionLostCallback;
        this.connectionRestoredCallback = connectionRestoredCallback;
    }

    public HivePrincipal getHivePrincipal() {
        connectionLock.readLock().lock();
        try {
            return hivePrincipal;
        } finally {
            connectionLock.readLock().unlock();
        }
    }

    protected abstract void beforeConnect()throws HiveException;

    protected abstract void doConnect() throws HiveException;

    protected abstract void afterConnect() throws HiveException;

    protected abstract void beforeDisconnect();

    protected abstract void doDisconnect();

    protected abstract void afterDisconnect();


    public final void connect() throws HiveException {
        connectionLock.writeLock().lock();
        try {
            beforeConnect();
            doConnect();
            afterConnect();
        } finally {
            connectionLock.writeLock().unlock();
        }
    }

    public final void disconnect() {
        connectionLock.writeLock().lock();
        try {
            beforeDisconnect();
            doDisconnect();
            afterDisconnect();
        } finally {
            connectionLock.writeLock().unlock();
        }
    }


    public void authenticate(HivePrincipal hivePrincipal) throws HiveException {
        connectionLock.writeLock().lock();
        try {
            if (this.hivePrincipal != null && !this.hivePrincipal.equals(hivePrincipal)) {
                throw new IllegalStateException(Messages.ALREADY_AUTHENTICATED);
            }
            this.hivePrincipal = hivePrincipal;
        } finally {
            connectionLock.writeLock().unlock();
        }
    }


    public final void close()  {
        connectionLock.writeLock().lock();
        try {
            disconnect();
        } finally {
            connectionLock.writeLock().unlock();
        }
    }
}
