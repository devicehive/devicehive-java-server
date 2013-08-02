package com.devicehive.messages.data.derby;

import java.sql.Connection;
import java.util.Collection;

import javax.annotation.sql.DataSourceDefinition;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devicehive.configuration.Constants;
import com.devicehive.messages.MessageType;
import com.devicehive.messages.bus.notify.StatefulNotifier;
import com.devicehive.messages.data.MessagesDataSource;
import com.devicehive.messages.data.derby.subscriptions.dao.CommandSubscriptionDAO;
import com.devicehive.messages.data.derby.subscriptions.dao.CommandUpdatesSubscriptionDAO;
import com.devicehive.messages.data.derby.subscriptions.dao.NotificationSubscriptionDAO;
import com.devicehive.messages.data.subscriptions.model.CommandUpdatesSubscription;
import com.devicehive.messages.data.subscriptions.model.CommandsSubscription;
import com.devicehive.model.Message;

@DataSourceDefinition(
        className = Constants.DATA_SOURCE_CLASS_NAME,
        name = Constants.DATA_SOURCE_NAME,
        databaseName = "memory:devicehive;create=true",
        transactional = true,
        isolationLevel = Connection.TRANSACTION_READ_COMMITTED,
        initialPoolSize = 2,
        minPoolSize = 2,
        maxPoolSize = 100)
@Singleton
@Alternative
public class DerbyDataSource implements MessagesDataSource {

    private static final Logger logger = LoggerFactory.getLogger(DerbyDataSource.class);

    @Inject
    private NotificationSubscriptionDAO notificationSubscriptionDAO;
    @Inject
    private CommandSubscriptionDAO commandSubscriptionDAO;
    @Inject
    private CommandUpdatesSubscriptionDAO commandUpdatesSubscriptionDAO;

    public DerbyDataSource() {
    }

    @Override
    public void addCommandsSubscription(String sessionId, Long deviceId) {
        if (sessionId != null) {
            logger.debug("Subscribing for commands for device : " + deviceId + " and session : " + sessionId);
            commandSubscriptionDAO.deleteByDevice(deviceId);
            commandSubscriptionDAO.insert(new CommandsSubscription(deviceId, sessionId));
        }
    }

    @Override
    public void removeCommandsSubscription(String sessionId, Long deviceId) {
        if (sessionId != null) {
            logger.debug("Unsubscribing from commands for device : " + deviceId + " and session : " + sessionId);
            commandSubscriptionDAO.deleteByDevice(deviceId);
        }
    }

    @Override
    public void removeCommandsSubscription(Long deviceId) {
        if (deviceId != null) {
            logger.debug("Unsubscribing from commands for device : " + deviceId);
            commandSubscriptionDAO.deleteByDevice(deviceId);
        }
    }

    @Override
    public void addCommandUpdatesSubscription(String sessionId, Long commandId) {
        if (sessionId != null) {
            logger.debug("Subscribing for commands update for command : " + commandId + " and session : " + sessionId);
            commandUpdatesSubscriptionDAO.deleteByCommandId(commandId);
            commandUpdatesSubscriptionDAO.insert(new CommandUpdatesSubscription(commandId, sessionId));
        }
    }

    @Override
    public void removeCommandUpdatesSubscription(Long commandId) {
        logger.debug("Unsubscribing from commands update for command : " + commandId);
        commandUpdatesSubscriptionDAO.deleteByCommandId(commandId);
    }

    @Override
    public void addNotificationsSubscription(String sessionId, Collection<Long> deviceIds) {
        notificationSubscriptionDAO.insertSubscriptions(deviceIds, sessionId);
    }

    @Override
    public void removeNotificationSubscriptions(String sessionId, Collection<Long> deviceIds) {
        if (deviceIds == null) {
            notificationSubscriptionDAO.deleteBySession(sessionId);
        }
        else if (!deviceIds.isEmpty()) {
            for (Long deviceId : deviceIds) {
                notificationSubscriptionDAO.deleteByDeviceAndSession(deviceId, sessionId);
            }
        }
    }

    @Override
    public void removeNotificationSubscription(Long deviceId) {
        notificationSubscriptionDAO.deleteByDevice(deviceId);
    }

    @Override
    public void removeDeviceSubscriptions(String sessionId) {
        commandSubscriptionDAO.deleteBySession(sessionId);
    }

    @Override
    public void removeClientSubscriptions(String sessionId) {
        commandUpdatesSubscriptionDAO.deleteBySession(sessionId);
        notificationSubscriptionDAO.deleteBySession(sessionId);
    }

    @Override
    public com.devicehive.messages.data.subscriptions.dao.CommandSubscriptionDAO commandSubscriptions() {
        return commandSubscriptionDAO;
    }

    @Override
    public com.devicehive.messages.data.subscriptions.dao.CommandUpdatesSubscriptionDAO commandUpdatesSubscriptions() {
        return commandUpdatesSubscriptionDAO;
    }

    @Override
    public com.devicehive.messages.data.subscriptions.dao.NotificationSubscriptionDAO notificationSubscriptions() {
        return notificationSubscriptionDAO;
    }

    @Override
    public InstallationType getType() {
        return InstallationType.SINGLE_NODE;
    }

    @Override
    public void init(StatefulNotifier notifier) {
    }

    @Override
    public void publish(Message message, MessageType messageType) {   
    }
}
