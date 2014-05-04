package com.devicehive.client.impl.context;


import com.devicehive.client.MessageHandler;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.SubscriptionFilter;
import com.devicehive.client.model.exceptions.HiveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entity that keeps all state, i.e. rest and websocket client, subscriptions info, transport to use.
 */
public class AbstractHiveContext {
    private static Logger logger = LoggerFactory.getLogger(AbstractHiveContext.class);

    private final MessageHandler<DeviceCommand> commandsHandler;
    private final MessageHandler<DeviceCommand> commandUpdatesHandler;
    private final MessageHandler<DeviceNotification> notificationsHandler;

    private HivePrincipal hivePrincipal;

    /**
     *
     * @param commandsHandler               handler for incoming commands and command updates
     * @param notificationsHandler          handler for incoming notifications
     */

    public AbstractHiveContext(MessageHandler<DeviceCommand> commandsHandler, MessageHandler<DeviceCommand> commandUpdatesHandler, MessageHandler<DeviceNotification> notificationsHandler) {
        this.commandsHandler = commandsHandler;
        this.commandUpdatesHandler = commandUpdatesHandler;
        this.notificationsHandler = notificationsHandler;
    }


    public MessageHandler<DeviceCommand> getCommandsHandler() {
        return commandsHandler;
    }

    public MessageHandler<DeviceNotification> getNotificationsHandler() {
        return notificationsHandler;
    }

    public MessageHandler<DeviceCommand> getCommandUpdatesHandler() {
        return commandUpdatesHandler;
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
     *
     * @param hivePrincipal hive principal with credentials.
     */
    public synchronized void authenticate(HivePrincipal hivePrincipal) throws HiveException {
        if (this.hivePrincipal != null && ! this.hivePrincipal.equals(hivePrincipal)) {
            throw new IllegalStateException("Already authenticated");
        }
        this.hivePrincipal = hivePrincipal;

    }

    public synchronized void close() {
        //noop
    }
}
