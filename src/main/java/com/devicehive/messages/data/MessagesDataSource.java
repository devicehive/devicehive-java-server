package com.devicehive.messages.data;

import java.util.Collection;

import com.devicehive.messages.bus.MessageBus;
import com.devicehive.messages.data.derby.DerbyDataSource;
import com.devicehive.messages.data.hash.HashBasedDataSource;
import com.devicehive.messages.data.subscriptions.dao.CommandSubscriptionDAO;
import com.devicehive.messages.data.subscriptions.dao.CommandUpdatesSubscriptionDAO;
import com.devicehive.messages.data.subscriptions.dao.NotificationSubscriptionDAO;

/**
 * Usually injected in {@link MessageBus} to represent storage for subscriptions.
 * There are 3 types of subscriptions: command, command-update and notification.
 * Each of them can be used to subscribe and unsubscribe to choosen type.
 * 
 * Add and remove methods should do what they say: 
 * <li>add subsciption, so later it can be used to notify subscribers.</li>
 * <li>remove subsciption, so subscribers will not receive any messages.</li>
 * 
 * Known implementations: {@link DerbyDataSource} and {@link HashBasedDataSource}.
 * 
 * @author rroschin
 *
 */
public interface MessagesDataSource {

    public void addCommandsSubscription(String sessionId, Long deviceId);

    public void removeCommandsSubscription(String sessionId, Long deviceId);

    public void removeCommandsSubscription(Long deviceId);

    public void addCommandUpdatesSubscription(String sessionId, Long commandId);

    public void removeCommandUpdatesSubscription(Long commandId);

    public void addNotificationsSubscription(String sessionId, Collection<Long> deviceIds);

    public void removeNotificationSubscriptions(String sessionId, Collection<Long> deviceIds);

    public void removeNotificationSubscription(Long deviceId);

    public void removeDeviceSubscriptions(String sessionId);

    public void removeClientSubscriptions(String sessionId);

    /**
     * Acessor
     * @return CommandSubscriptionDAO implementation
     */
    public CommandSubscriptionDAO commandSubscriptions();

    /**
     * Acessor
     * @return CommandUpdatesSubscriptionDAO implementation
     */
    public CommandUpdatesSubscriptionDAO commandUpdatesSubscriptions();

    /**
     * Acessor
     * @return NotificationSubscriptionDAO implementation
     */
    public NotificationSubscriptionDAO notificationSubscriptions();

}
