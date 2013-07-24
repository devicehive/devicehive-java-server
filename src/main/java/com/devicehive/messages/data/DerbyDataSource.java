package com.devicehive.messages.data;

import java.sql.Connection;
import java.util.Collection;

import javax.annotation.sql.DataSourceDefinition;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devicehive.configuration.Constants;
import com.devicehive.messages.data.subscriptions.dao.CommandSubscriptionDAO;
import com.devicehive.messages.data.subscriptions.dao.CommandUpdatesSubscriptionDAO;
import com.devicehive.messages.data.subscriptions.dao.NotificationSubscriptionDAO;
import com.devicehive.messages.data.subscriptions.model.CommandUpdatesSubscription;
import com.devicehive.messages.data.subscriptions.model.CommandsSubscription;

@DataSourceDefinition(
        className = Constants.DATA_SOURCE_CLASS_NAME,
        name = Constants.DATA_SOURCE_NAME,
        databaseName = "memory:devicehive;create=true",
        transactional = true,
        isolationLevel = Connection.TRANSACTION_READ_COMMITTED,
        initialPoolSize = 2,
        minPoolSize = 2,
        maxPoolSize = 100)
@Stateless
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
        logger.debug("Subscribing for commands for device : " + deviceId + " and session : " + sessionId);
        commandSubscriptionDAO.deleteByDevice(deviceId);
        commandSubscriptionDAO.insert(new CommandsSubscription(deviceId, sessionId));
    }

    @Override
    public void removeCommandsSubscription(String sessionId, Long deviceId) {
        logger.debug("Unsubscribing from commands for device : " + deviceId + " and session : " + sessionId);
        commandSubscriptionDAO.deleteByDevice(deviceId);
    }

    @Override
    public void addCommandUpdatesSubscription(String sessionId, Long commandId) {
        logger.debug("Subscribing for commands update for command : " + commandId + " and session : " + sessionId);
        commandUpdatesSubscriptionDAO.deleteByCommandId(commandId);
        commandUpdatesSubscriptionDAO.insert(new CommandUpdatesSubscription(commandId, sessionId));
    }

    @Override
    public void removeCommandUpdatesSubscription(String sessionId, Long commandId) {
        logger.debug("Unsubscribing from commands update for command : " + commandId + " and session : " + sessionId);
        commandUpdatesSubscriptionDAO.deleteByCommandId(commandId);
    }

    @Override
    public void addNotificationsSubscription(String sessionId, Collection<Long> deviceIds) {
        notificationSubscriptionDAO.insertSubscriptions(deviceIds, sessionId);
    }

    @Override
    public void removeNotificationsSubscription(String sessionId, Collection<Long> deviceIds) {
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
    public void removeCommandsSubscriptions(String sessionId) {
        commandSubscriptionDAO.deleteBySession(sessionId);
    }

    @Override
    public void removeCommandUpdatesSubscriptions(String sessionId) {
        commandUpdatesSubscriptionDAO.deleteBySession(sessionId);
        notificationSubscriptionDAO.deleteBySession(sessionId);
    }

}
