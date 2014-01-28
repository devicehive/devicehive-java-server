package com.devicehive.client.impl.rest.subs;


import com.devicehive.client.impl.context.Constants;
import com.devicehive.client.impl.context.HiveContext;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class RestSubManager {

    private static Logger logger = LoggerFactory.getLogger(RestSubManager.class);

    private final HiveContext hiveContext;
    private final ExecutorService subscriptionExecutor = Executors.newCachedThreadPool();

    private Map<Pair<String, Set<String>>, Future<?>> commandsSubscriptionsStorage = new HashMap<>();
    private Map<Pair<String, Set<String>>, Future<?>> notificationsSubscriptionsStorage = new HashMap<>();


    public RestSubManager(HiveContext hiveContext) {
        this.hiveContext = hiveContext;
    }

    /**
     * Adds commands subscription to storage. Creates task that store commands in context's command queue. In case
     * when no device identifiers specified, subscription "for all available" will be added.
     *
     * @param headers   headers that defines the sample of commands
     * @param timestamp first command timestamp
     * @param names     names of commands that defines
     * @param deviceIds devices identifiers of devices that should be subscribed
     */
    public synchronized void addCommandsSubscription(Map<String, String> headers, Timestamp timestamp,
                                                     Set<String> names, String... deviceIds) {
        if (deviceIds == null) {
            Pair<String, Set<String>> key = ImmutablePair.of(Constants.FOR_ALL_SUBSTITUTE, names);
            if (!commandsSubscriptionsStorage.containsKey(key)) {
                Future subscription = subscriptionExecutor.submit(new AllDeviceCommandRestSubscription(hiveContext, timestamp, 60, headers, names));
                commandsSubscriptionsStorage.put(key, subscription);
                logger.debug("New subscription added for: {}", Constants.FOR_ALL_SUBSTITUTE);
            }
        } else {
            for (String id : deviceIds) {
                Pair<String, Set<String>> key = ImmutablePair.of(id, names);
                if (!commandsSubscriptionsStorage.containsKey(key)) {
                    Future subscription = subscriptionExecutor.submit(new SingleDeviceCommandRestSubscription(hiveContext, timestamp, 60, headers, names, id));
                    commandsSubscriptionsStorage.put(key, subscription);
                    logger.debug("New subscription added for device with id: {}", id);
                }
            }
        }
    }

    /**
     * Put command updates into the queue as soon as update coming. Command update subscription adds when the command
     * insert executes.
     *
     * @param commandId command identifier
     * @param deviceId  device identifier
     */
    public synchronized void addCommandUpdateSubscription(long commandId, String deviceId) {
        Future subscription = subscriptionExecutor.submit(new CommandUpdateRestSubscription(hiveContext, 60, Collections.<String, String>emptyMap(), deviceId, commandId));
        logger.debug("New subscription added for device with id: {} and command id: {}", deviceId, commandId);
    }

    /**
     * Remove command subscription for following command name and device identifier. In case when no device identifiers specified,
     * surrogate subscription "for all available" will be removed. This subscription does not
     * include subscriptions for specific device.
     *
     * @param names     set of command names
     * @param deviceIds device identifiers.
     */
    public synchronized void removeCommandSubscription(Set<String> names, String... deviceIds) {
        if (deviceIds == null) {
            Pair<String, Set<String>> key = ImmutablePair.of(Constants.FOR_ALL_SUBSTITUTE, names);
            Future subscription = commandsSubscriptionsStorage.remove(key);
            if (subscription != null) {
                subscription.cancel(true);
            }
        } else {
            for (String id : deviceIds) {
                Pair<String, Set<String>> key = ImmutablePair.of(id, names);
                Future subscription = commandsSubscriptionsStorage.remove(key);
                if (subscription != null) {
                    subscription.cancel(true);
                }
            }
        }
    }

    /**
     * Adds subscription for notifications with following set of notification's names from device with defined device
     * identifiers. In case when no device identifiers specified, subscription for all available devices will be added.
     *
     * @param headers   headers that define the sample of commands
     * @param timestamp start timestamp
     * @param names     notifications names (statistics)
     * @param deviceIds device identifiers
     */
    public synchronized void addNotificationSubscription(Map<String, String> headers, Timestamp timestamp, Set<String> names,
                                                         String... deviceIds) {
        if (deviceIds == null) {
            Pair<String, Set<String>> key = ImmutablePair.of(Constants.FOR_ALL_SUBSTITUTE, names);
            if (!notificationsSubscriptionsStorage.containsKey(key)) {
                Future subscription = subscriptionExecutor.submit(new AllDeviceNotificationRestSubscription(hiveContext, timestamp, 60, headers, names));
                notificationsSubscriptionsStorage.put(key, subscription);
                logger.debug("New subscription added for: {}", Constants.FOR_ALL_SUBSTITUTE);
            }
        } else {
            for (String id : deviceIds) {
                Pair<String, Set<String>> key = ImmutablePair.of(id, names);
                if (!notificationsSubscriptionsStorage.containsKey(key)) {
                    Future subscription = subscriptionExecutor.submit(new SingleDeviceNotificationRestSubscription(hiveContext, timestamp, 60, headers, names, id));
                    notificationsSubscriptionsStorage.put(key, subscription);
                    logger.debug("New subscription added for device with id: {}", id);
                }
            }
        }
    }

    /**
     * Remove notification subscription for following notification name and device identifier. In case when no device
     * identifiers specified, surrogate subscription "for all available" will be removed. This subscription does not
     * include subscriptions for specific device.
     *
     * @param names     set of notification names
     * @param deviceIds device identifiers.
     */
    public synchronized void removeNotificationSubscription(Set<String> names, String... deviceIds) {
        if (deviceIds == null) {
            Pair<String, Set<String>> key = ImmutablePair.of(Constants.FOR_ALL_SUBSTITUTE, names);
            Future subscription = notificationsSubscriptionsStorage.remove(key);
            if (subscription != null) {
                subscription.cancel(true);
            }
        } else {
            for (String id : deviceIds) {
                Pair<String, Set<String>> key = ImmutablePair.of(id, names);
                Future subscription = notificationsSubscriptionsStorage.remove(key);
                if (subscription != null) {
                    subscription.cancel(true);
                }
            }
        }
    }


    public synchronized void resubscribeAll() {

    }


    public void close() {
        subscriptionExecutor.shutdownNow();
    }
}
