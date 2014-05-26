package com.devicehive.client.impl.context;


import com.devicehive.client.HiveMessageHandler;
import com.devicehive.client.Status;
import com.devicehive.client.impl.util.Messages;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.exceptions.HiveException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class AbstractHiveAgent {

    private final ReadWriteLock statusLock = new ReentrantReadWriteLock(true);
    private final ConcurrentMap<Long, HiveMessageHandler<DeviceCommand>> commandUpdatesHandlerStorage =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<String, SubscriptionDescriptor<DeviceCommand>> commandSubscriptionsStorage =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<String, SubscriptionDescriptor<DeviceNotification>> notificationSubscriptionsStorage =
            new ConcurrentHashMap<>();
    //the first String stands for old subscription identifier, the second one stands for new subscription identifier
    private final ConcurrentMap<String, String> oldNewSubIds = new ConcurrentHashMap<>();
    private volatile Status status;
    private HivePrincipal hivePrincipal;

    public ConcurrentMap<String, SubscriptionDescriptor<DeviceCommand>> getCommandSubscriptionsStorage() {
        return commandSubscriptionsStorage;
    }

    public ConcurrentMap<String, SubscriptionDescriptor<DeviceNotification>> getNotificationSubscriptionsStorage() {
        return notificationSubscriptionsStorage;
    }

    public HiveMessageHandler<DeviceCommand> getCommandUpdatesHandler(Long commandId) {
        return commandUpdatesHandlerStorage.get(commandId);
    }

    public void removeCommandUpdatesHandler(Long commandId) {
        commandUpdatesHandlerStorage.remove(commandId);
    }

    public SubscriptionDescriptor<DeviceCommand> getCommandsSubscriptionDescriptor(String subscriptionId) {
        return commandSubscriptionsStorage.get(oldNewSubIds.get(subscriptionId));
    }

    public SubscriptionDescriptor<DeviceNotification> getNotificationsSubscriptionDescriptor(String subscriptionId) {
        return notificationSubscriptionsStorage.get(oldNewSubIds.get(subscriptionId));
    }

    public void addCommandsSubscription(String newSubscriptionId,
                                        SubscriptionDescriptor<DeviceCommand> commandsHandler) {
        commandSubscriptionsStorage.put(newSubscriptionId, commandsHandler);
        oldNewSubIds.putIfAbsent(newSubscriptionId, newSubscriptionId);
    }

    protected void replaceCommandSubscription(String oldSubscriptionId,
                                              String newSubscriptionId,
                                              SubscriptionDescriptor<DeviceCommand> commandsHandler) {
        commandSubscriptionsStorage.remove(oldSubscriptionId);
        commandSubscriptionsStorage.put(newSubscriptionId, commandsHandler);
        oldNewSubIds.replace(oldSubscriptionId, newSubscriptionId);
    }

    protected void replaceNotificationSubscription(String oldSubscriptionId,
                                                   String newSubscriptionId,
                                                   SubscriptionDescriptor<DeviceNotification> notificationsHandler) {
        notificationSubscriptionsStorage.remove(oldSubscriptionId);
        notificationSubscriptionsStorage.put(newSubscriptionId, notificationsHandler);
        oldNewSubIds.replace(oldSubscriptionId, newSubscriptionId);
    }

    public void addNotificationsSubscription(String subscriptionId,
                                             SubscriptionDescriptor<DeviceNotification> notificationsHandler) {
        notificationSubscriptionsStorage.put(subscriptionId, notificationsHandler);
    }

    public void removeCommandsSubscription(String subscriptionId) throws HiveException {
        commandSubscriptionsStorage.remove(subscriptionId);
    }

    public void removeNotificationsSubscription(String subscriptionId) throws HiveException {
        notificationSubscriptionsStorage.remove(subscriptionId);
    }

    public synchronized HivePrincipal getHivePrincipal() {
        return hivePrincipal;
    }

    public Status getStatus() {
        return status;
    }

    protected void setStatus(Status status) {
        this.status = status;
    }

    protected ReadWriteLock getStatusLock() {
        return statusLock;
    }

    protected abstract void beforeConnect() throws HiveException;

    protected abstract void doConnect() throws HiveException;

    protected abstract void afterConnect() throws HiveException;

    protected abstract void beforeDisconnect() throws HiveException;

    protected abstract void doDisconnect() throws HiveException;

    protected abstract void afterDisconnect() throws HiveException;

    protected abstract void resubscribe() throws HiveException;

    public synchronized final void connect() throws HiveException {
        beforeConnect();
        setStatus(Status.CONNECTING);
        doConnect();
        setStatus(Status.CONNECTED);
        afterConnect();
    }

    public synchronized final void disconnect() throws HiveException {
        beforeDisconnect();
        doDisconnect();
        setStatus(Status.NOT_CONNECTED);
        afterDisconnect();
    }

    public synchronized final void reconnect() throws HiveException {
        disconnect();
        connect();
    }

    public synchronized void authenticate(HivePrincipal hivePrincipal) throws HiveException {
        if (this.hivePrincipal != null && !this.hivePrincipal.equals(hivePrincipal)) {
            throw new IllegalStateException(Messages.ALREADY_AUTHENTICATED);
        }
        this.hivePrincipal = hivePrincipal;
    }

    public synchronized void close() throws HiveException {
        disconnect();
    }
}
