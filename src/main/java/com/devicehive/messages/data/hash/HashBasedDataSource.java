package com.devicehive.messages.data.hash;

import java.util.Collection;

import javax.ejb.Singleton;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devicehive.messages.data.MessagesDataSource;
import com.devicehive.messages.data.hash.subscriptions.dao.CommandSubscriptionDAO;
import com.devicehive.messages.data.hash.subscriptions.dao.CommandUpdatesSubscriptionDAO;
import com.devicehive.messages.data.hash.subscriptions.dao.NotificationSubscriptionDAO;
import com.devicehive.messages.data.subscriptions.model.CommandUpdatesSubscription;
import com.devicehive.messages.data.subscriptions.model.CommandsSubscription;

/**
 * Implementation of {@link MessagesDataSource}. Stores all subscription data in hash-based collections.
 * Thread-safe.
 * 
 * Can be used later to implement cluster-case environment with <a href="http://www.hazelcast.com/">
 * 
 * @author rroschin
 *
 */
@Singleton
@HashMapBased
public class HashBasedDataSource implements MessagesDataSource {

    private static final Logger logger = LoggerFactory.getLogger(HashBasedDataSource.class);

    @Inject
    private NotificationSubscriptionDAO notificationSubscriptionDAO;
    @Inject
    private CommandSubscriptionDAO commandSubscriptionDAO;
    @Inject
    private CommandUpdatesSubscriptionDAO commandUpdatesSubscriptionDAO;

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

}