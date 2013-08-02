package com.devicehive.messages.data.cluster.hazelcast;

import java.util.Collection;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devicehive.messages.MessageType;
import com.devicehive.messages.bus.notify.StatefulNotifier;
import com.devicehive.messages.data.MessagesDataSource;
import com.devicehive.messages.data.cluster.hazelcast.subscriptions.dao.CommandSubscriptionDAO;
import com.devicehive.messages.data.cluster.hazelcast.subscriptions.dao.CommandUpdatesSubscriptionDAO;
import com.devicehive.messages.data.cluster.hazelcast.subscriptions.dao.NotificationSubscriptionDAO;
import com.devicehive.messages.data.subscriptions.model.CommandUpdatesSubscription;
import com.devicehive.messages.data.subscriptions.model.CommandsSubscription;
import com.devicehive.model.Message;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

/**
 * Implementation of {@link MessagesDataSource}. Stores all subscription data in hazelcast collections
 * This is cluster implementation of {@link MessagesDataSource}. 
 * 
 * @author rroschin
 *
 */
@Singleton
@Startup
@Alternative
public class HazelcastDataSource implements MessagesDataSource {

    private static final Logger logger = LoggerFactory.getLogger(HazelcastDataSource.class);

    @Inject
    private NotificationSubscriptionDAO notificationSubscriptionDAO;
    @Inject
    private CommandSubscriptionDAO commandSubscriptionDAO;
    @Inject
    private CommandUpdatesSubscriptionDAO commandUpdatesSubscriptionDAO;

    private HazelcastInstance hazelcast;

    public HazelcastDataSource() {
        if (hazelcast != null) {
            return;
        }

        logger.debug("Initializing Hazelcast instance...");
        Config config = new Config();
        config.setProperty("hazelcast.logging.type", "slf4j");
        hazelcast = Hazelcast.newHazelcastInstance(config);
        logger.debug("New Hazelcast instance created: " + hazelcast);
    }

    @Override
    public void init(StatefulNotifier statefulNotifier) {
        commandSubscriptionDAO.setHazelcast(hazelcast);
        commandUpdatesSubscriptionDAO.setHazelcast(hazelcast);
        notificationSubscriptionDAO.setHazelcast(hazelcast);

        hazelcast.getTopic(MessageType.CLIENT_TO_DEVICE_COMMAND.toString()).addMessageListener(
                new StatefulMessageListener(MessageType.CLIENT_TO_DEVICE_COMMAND, statefulNotifier));
        hazelcast.getTopic(MessageType.DEVICE_TO_CLIENT_UPDATE_COMMAND.toString()).addMessageListener(
                new StatefulMessageListener(MessageType.DEVICE_TO_CLIENT_UPDATE_COMMAND, statefulNotifier));
        hazelcast.getTopic(MessageType.DEVICE_TO_CLIENT_NOTIFICATION.toString()).addMessageListener(
                new StatefulMessageListener(MessageType.DEVICE_TO_CLIENT_NOTIFICATION, statefulNotifier));
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
        logger.debug("Subscribing for notifications for device ids: " + deviceIds + " and session : " + sessionId);
        notificationSubscriptionDAO.insertSubscriptions(deviceIds, sessionId);
    }

    @Override
    public void removeNotificationSubscriptions(String sessionId, Collection<Long> deviceIds) {
        logger.debug("Unsubscribing from notification for device ids : " + deviceIds + " and session " + sessionId);
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
        logger.debug("Unsubscribing from notification for deviceId : " + deviceId);
        notificationSubscriptionDAO.deleteByDevice(deviceId);
    }

    @Override
    public void removeDeviceSubscriptions(String sessionId) {
        logger.debug("Unsubscribing Device from all subscription for session " + sessionId);
        commandSubscriptionDAO.deleteBySession(sessionId);
    }

    @Override
    public void removeClientSubscriptions(String sessionId) {
        logger.debug("Unsubscribing Client from all subscription for session " + sessionId);
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
    public void publish(Message message, MessageType messageType) {
        hazelcast.getTopic(messageType.toString()).publish(message);
    }

    @Override
    public InstallationType getType() {
        return InstallationType.CLUSTER;
    }

}