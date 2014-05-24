package com.devicehive.client.impl.context;


import com.devicehive.client.HiveMessageHandler;
import com.devicehive.client.impl.util.Messages;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.exceptions.HiveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Entity that keeps all state, i.e. rest and websocket client, subscriptions info, transport to use.
 */
class AbstractHiveContext {
    private static Logger logger = LoggerFactory.getLogger(AbstractHiveContext.class);
    private final HiveMessageHandler<DeviceCommand> commandUpdatesHandler;
    private final Map<String, HiveMessageHandler<DeviceCommand>> commandSubscriptionsStorage;
    private final Map<String, HiveMessageHandler<DeviceNotification>> notificationSubscriptionsStorage;
    private HivePrincipal hivePrincipal;

    /**
     * @param commandUpdatesHandler handler for incoming command updates
     */

    public AbstractHiveContext(HiveMessageHandler<DeviceCommand> commandUpdatesHandler) {
        this.commandUpdatesHandler = commandUpdatesHandler;
        commandSubscriptionsStorage = new HashMap<>();
        notificationSubscriptionsStorage = new HashMap<>();
    }

    public HiveMessageHandler<DeviceCommand> getCommandUpdatesHandler() {
        return commandUpdatesHandler;
    }

    public HiveMessageHandler<DeviceCommand> getCommandsHandler(String subscriptionId) {
        return commandSubscriptionsStorage.get(subscriptionId);
    }

    public HiveMessageHandler<DeviceNotification> getNotificationsHandler(String subscriptionId) {
        return notificationSubscriptionsStorage.get(subscriptionId);
    }

    public void addCommandsSubscription(String subscriptionId, HiveMessageHandler<DeviceCommand> commandsHandler) {
        commandSubscriptionsStorage.put(subscriptionId, commandsHandler);
    }

    public void addNotificationsSubscription(String subscriptionId, HiveMessageHandler<DeviceNotification>
            notificationsHandler) {
        notificationSubscriptionsStorage.put(subscriptionId, notificationsHandler);
    }

    public void removeCommandsSubscription(String subscriptionId) throws HiveException {
        commandSubscriptionsStorage.remove(subscriptionId);
    }

    public void removeNotificationsSubscription(String subscriptionId) throws HiveException {
        notificationSubscriptionsStorage.remove(subscriptionId);
    }

    /**
     * Get hive principal (credentials storage).
     *
     * @return hive principal
     */
    public synchronized HivePrincipal getHivePrincipal() {
        return hivePrincipal;
    }

    /**
     * @param hivePrincipal hive principal with credentials.
     */
    public synchronized void authenticate(HivePrincipal hivePrincipal) throws HiveException {
        if (this.hivePrincipal != null && !this.hivePrincipal.equals(hivePrincipal)) {
            throw new IllegalStateException(Messages.ALREADY_AUTHENTICATED);
        }
        this.hivePrincipal = hivePrincipal;

    }

    public synchronized void close() {
        //noop
    }
}
