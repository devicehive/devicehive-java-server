package com.devicehive.client.impl.rest.subs;


import com.devicehive.client.impl.context.Constants;
import com.devicehive.client.impl.context.HiveContext;
import com.devicehive.client.model.DevicesNamesFilter;
import com.devicehive.client.model.SubscriptionFilter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class RestSubManager {

    private static Logger logger = LoggerFactory.getLogger(RestSubManager.class);
    private static final int TIMEOUT = 60;

    private final HiveContext hiveContext;
    private final ExecutorService subscriptionExecutor = Executors.newCachedThreadPool();
    private Future commandsSubscription;
    private Future notificationsubscription;


    public RestSubManager(HiveContext hiveContext) {
        this.hiveContext = hiveContext;
    }

    /**
     * Adds commands subscription to storage. Creates task that store commands in context's command queue. In case
     * when no device identifiers specified, subscription "for all available" will be added.
     */
    public synchronized void addCommandsSubscription(SubscriptionFilter filter) {
        removeCommandSubscription();
        commandsSubscription = subscriptionExecutor
                .submit(new CommandRestSubscription(hiveContext, filter, TIMEOUT));
    }

    /**
     * Put command updates into the queue as soon as update coming. Command update subscription adds when the command
     * insert executes.
     *
     * @param commandId command identifier
     * @param deviceId  device identifier
     */
    public synchronized void addCommandUpdateSubscription(long commandId, String deviceId) {
        subscriptionExecutor.submit(new CommandUpdateRestSubscription(hiveContext, TIMEOUT, deviceId, commandId));
    }

    /**
     * Remove command subscription for all available commands.
     */
    public synchronized void removeCommandSubscription() {
        if (commandsSubscription != null) {
            commandsSubscription.cancel(true);
            commandsSubscription = null;
        }
    }

    /**
     * Adds subscription for notifications with following set of notification's names from device with defined device
     * identifiers. In case when no device identifiers specified, subscription for all available devices will be added.
     *
     */
    public synchronized void addNotificationSubscription(SubscriptionFilter filter) {
        removeNotificationSubscription();
        notificationsubscription = subscriptionExecutor
                .submit(new NotificationRestSubscription(hiveContext, filter, TIMEOUT));
    }

    /**
     * Remove all previous notification subscriptions.
     */
    public synchronized void removeNotificationSubscription() {
        if (notificationsubscription != null) {
            notificationsubscription.cancel(true);
            notificationsubscription = null;
        }
    }


    public synchronized void close() {
        removeCommandSubscription();
        removeNotificationSubscription();
        subscriptionExecutor.shutdownNow();
    }
}
