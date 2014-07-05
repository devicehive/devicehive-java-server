package com.devicehive.client.impl.context;


import com.devicehive.client.impl.Status;
import com.devicehive.client.impl.util.Messages;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.exceptions.HiveException;
import com.google.common.base.Preconditions;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractHiveAgent {

    private final ConcurrentMap<String, SubscriptionDescriptor<DeviceCommand>> commandSubscriptionsStorage =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<String, SubscriptionDescriptor<DeviceNotification>> notificationSubscriptionsStorage =
            new ConcurrentHashMap<>();
    //the first String stands for old subscription identifier, the second one stands for new subscription identifier
    private final ConcurrentMap<String, String> oldNewSubIds = new ConcurrentHashMap<>();
    private volatile Status status;
    private HivePrincipal hivePrincipal;

    protected final ConcurrentMap<String, SubscriptionDescriptor<DeviceCommand>> getCommandSubscriptionsStorage() {
        return commandSubscriptionsStorage;
    }

    protected final ConcurrentMap<String, SubscriptionDescriptor<DeviceNotification>> getNotificationSubscriptionsStorage() {
        return notificationSubscriptionsStorage;
    }


    protected final SubscriptionDescriptor<DeviceCommand> getCommandsSubscriptionDescriptor(String subscriptionId) {
        return commandSubscriptionsStorage.get(oldNewSubIds.get(subscriptionId));
    }

    protected final SubscriptionDescriptor<DeviceNotification> getNotificationsSubscriptionDescriptor(String subscriptionId) {
        return notificationSubscriptionsStorage.get(oldNewSubIds.get(subscriptionId));
    }

    protected final void addCommandsSubscription(String newSubscriptionId,
                                                 SubscriptionDescriptor<DeviceCommand> descriptor) {
        Preconditions.checkState(!oldNewSubIds.containsKey(newSubscriptionId));
        commandSubscriptionsStorage.put(newSubscriptionId, descriptor);
        oldNewSubIds.put(newSubscriptionId, newSubscriptionId);
    }

    protected final void replaceCommandSubscription(String oldSubscriptionId,
                                                    String newSubscriptionId,
                                                    SubscriptionDescriptor<DeviceCommand> descriptor) {
        Preconditions.checkState(oldNewSubIds.containsKey(oldSubscriptionId));
        Preconditions.checkState(!oldNewSubIds.containsKey(newSubscriptionId));
        commandSubscriptionsStorage.remove(oldSubscriptionId);
        commandSubscriptionsStorage.put(newSubscriptionId, descriptor);
        oldNewSubIds.replace(oldSubscriptionId, newSubscriptionId);
    }

    protected final void replaceNotificationSubscription(String oldSubscriptionId,
                                                         String newSubscriptionId,
                                                         SubscriptionDescriptor<DeviceNotification> descriptor) {
        Preconditions.checkState(oldNewSubIds.containsKey(oldSubscriptionId));
        Preconditions.checkState(!oldNewSubIds.containsKey(newSubscriptionId));
        notificationSubscriptionsStorage.remove(oldSubscriptionId);
        notificationSubscriptionsStorage.put(newSubscriptionId, descriptor);
        oldNewSubIds.replace(oldSubscriptionId, newSubscriptionId);
    }

    protected final void addNotificationsSubscription(String subscriptionId,
                                                      SubscriptionDescriptor<DeviceNotification> descriptor) {
        Preconditions.checkState(!oldNewSubIds.containsKey(subscriptionId));
        notificationSubscriptionsStorage.put(subscriptionId, descriptor);
        oldNewSubIds.put(subscriptionId, subscriptionId);
    }

    protected final void removeCommandsSubscription(String subscriptionId) throws HiveException {
        commandSubscriptionsStorage.remove(subscriptionId);
    }

    protected final void removeNotificationsSubscription(String subscriptionId) throws HiveException {
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


    protected abstract void beforeConnect()throws HiveException;

    protected abstract void doConnect() throws HiveException;

    protected abstract void afterConnect() throws HiveException;

    protected abstract void beforeDisconnect();

    protected abstract void doDisconnect();

    protected abstract void afterDisconnect();


    public synchronized final void connect() throws HiveException {
        beforeConnect();
        setStatus(Status.CONNECTING);
        doConnect();
        setStatus(Status.CONNECTED);
        afterConnect();
    }

    public synchronized final void disconnect() {
        beforeDisconnect();
        doDisconnect();
        setStatus(Status.NOT_CONNECTED);
        afterDisconnect();
    }


    public synchronized void authenticate(HivePrincipal hivePrincipal) throws HiveException {
        if (this.hivePrincipal != null && !this.hivePrincipal.equals(hivePrincipal)) {
            throw new IllegalStateException(Messages.ALREADY_AUTHENTICATED);
        }
        this.hivePrincipal = hivePrincipal;
    }

    public synchronized void close()  {
        disconnect();
    }
}
