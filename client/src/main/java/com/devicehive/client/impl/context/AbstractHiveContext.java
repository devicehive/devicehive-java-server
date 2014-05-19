package com.devicehive.client.impl.context;


import com.devicehive.client.MessageHandler;
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
public class AbstractHiveContext {
    private static Logger logger = LoggerFactory.getLogger(AbstractHiveContext.class);
    private final MessageHandler<DeviceCommand> commandUpdatesHandler;
    private final Map<String, MessageHandler<DeviceCommand>> commandSubscriptionsStorage;
    private final Map<String, MessageHandler<DeviceNotification>> notificationSubscriptionsStorage;
    private HivePrincipal hivePrincipal;

    /**
     * @param commandUpdatesHandler handler for incoming command updates
     */

    public AbstractHiveContext(MessageHandler<DeviceCommand> commandUpdatesHandler) {
        this.commandUpdatesHandler = commandUpdatesHandler;
        commandSubscriptionsStorage = new HashMap<>();
        notificationSubscriptionsStorage = new HashMap<>();
    }

    public MessageHandler<DeviceCommand> getCommandUpdatesHandler() {
        return commandUpdatesHandler;
    }

    public MessageHandler<DeviceCommand> getCommandsHandler(String subscriptionId) {
        return commandSubscriptionsStorage.get(subscriptionId);
    }

    public MessageHandler<DeviceNotification> getNotificationsHandler(String subscriptionId) {
        return notificationSubscriptionsStorage.get(subscriptionId);
    }

    public void addCommandsSubscription(String subscriptionId, MessageHandler<DeviceCommand> commandsHandler) {
        commandSubscriptionsStorage.put(subscriptionId, commandsHandler);
    }

    public void addNotificationsSubscription(String subscriptionId, MessageHandler<DeviceNotification>
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
            throw new IllegalStateException("Already authenticated");
        }
        this.hivePrincipal = hivePrincipal;

    }

    public synchronized void close() {
        //noop
    }
}
