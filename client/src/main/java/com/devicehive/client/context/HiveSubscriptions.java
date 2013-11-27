package com.devicehive.client.context;


import com.devicehive.client.api.SubscriptionsService;
import com.devicehive.client.config.Constants;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.util.SubscriptionTask;
import com.devicehive.client.util.UpdatesSubscriptionTask;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.COMMAND_TO_DEVICE;

/**
 * Here is all logic that connected with subscribe/unsubscribe actions.
 * Represents commands updates storage, commands storage and notifications storage. Keeps implementation of
 * subscribe/unsubscribe actions.
 */
public class HiveSubscriptions {

    private static final int SUBSCRIPTIONS_THREAD_POOL_SIZE = 100;
    private static final Integer AWAIT_TERMINATION_TIMEOUT = 10;
    private static final Integer CLEANER_TASK_INTERVAL = 30;// in minutes
    private static Logger logger = LoggerFactory.getLogger(HiveSubscriptions.class);
    private final HiveContext hiveContext;
    private ExecutorService subscriptionExecutor = Executors.newFixedThreadPool(SUBSCRIPTIONS_THREAD_POOL_SIZE);
    private Map<Pair<String, Set<String>>, Future<Void>> commandsSubscriptionsStorage = new HashMap<>();
    private Map<Pair<String, Set<String>>, Future<Void>> notificationsSubscriptionsStorage = new HashMap<>();
    private Map<Pair<String, Long>, Future<DeviceCommand>> commandsUpdateSubscriptionStorage = new HashMap<>();
    private ReadWriteLock rwCommandsLock = new ReentrantReadWriteLock();
    private ReadWriteLock rwCommandUpdateLock = new ReentrantReadWriteLock();
    private ReadWriteLock rwNotificationsLock = new ReentrantReadWriteLock();
    private Set<Pair<String, String>> wsCommandSubscriptionsStorage = new HashSet<>();
    private Set<Pair<String, String>> wsNotificationSubscriptionsStorage = new HashSet<>();
    private Map<Long, String> wsCommandUpdateSubscriptionsStorage = new HashMap<>();
    private Map<String, Timestamp> wsDeviceLastCommandTimestampAssociation = new HashMap<>();
    private Map<String, Timestamp> wsDeviceLastNotificationTimestampAssociation = new HashMap<>();
    private ReadWriteLock rwWsCommandsLock = new ReentrantReadWriteLock();
    private ReadWriteLock rwWsCommandUpdateLock = new ReentrantReadWriteLock();
    private ReadWriteLock rwWsNotificationsLock = new ReentrantReadWriteLock();
    //once in 30 minutes clean associations to avoid memory leak
    private ScheduledExecutorService wsAssociationsCleaner = Executors.newSingleThreadScheduledExecutor();

    public HiveSubscriptions(HiveContext hiveContext) {
        this.hiveContext = hiveContext;
    }

    private void clean() {
        //TODO
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
    public void addCommandsSubscription(Map<String, String> headers, Timestamp timestamp,
                                        Set<String> names, String... deviceIds) {
        if (deviceIds == null) {
            try {
                rwCommandsLock.writeLock().lock();
                if (!commandsSubscriptionsStorage.containsKey(ImmutablePair.of(Constants.FOR_ALL_SUBSTITUTE, names))) {
                    String path = "/device/command/poll";
                    SubscriptionTask task = new SubscriptionTask(hiveContext, timestamp, Constants.WAIT_TIMEOUT,
                            path, headers, names, Constants.FOR_ALL_SUBSTITUTE, DeviceCommand.class);
                    Future<Void> subscription = subscriptionExecutor.submit(task);
                    commandsSubscriptionsStorage.put(ImmutablePair.of(Constants.FOR_ALL_SUBSTITUTE, names),
                            subscription);
                    logger.debug("New subscription added for:" + Constants.FOR_ALL_SUBSTITUTE);
                }
            } finally {
                rwCommandsLock.writeLock().unlock();
            }
        } else {
            try {
                rwCommandsLock.writeLock().lock();
                for (String id : deviceIds) {
                    Future<Void> subscription = commandsSubscriptionsStorage.get(ImmutablePair.of(id, names));
                    if (subscription == null || subscription.isDone()) { //Returns true if this task completed.
                        // Completion may be due to normal termination, an exception, or cancellation --
                        // in all of these cases, this method will return true.
                        String path = "/device/" + id + "/command/poll";
                        SubscriptionTask task = new SubscriptionTask(hiveContext, timestamp, Constants.WAIT_TIMEOUT,
                                path, headers, names, id, DeviceCommand.class);
                        subscription = subscriptionExecutor.submit(task);
                        commandsSubscriptionsStorage.put(ImmutablePair.of(id, names), subscription);
                        logger.debug("New subscription added for device with id:" + id);
                    }
                }
            } finally {
                rwCommandsLock.writeLock().unlock();
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
    public void addCommandUpdateSubscription(long commandId, String deviceId) {
        try {
            rwCommandUpdateLock.writeLock().lock();
            Future<DeviceCommand> subscription =
                    commandsUpdateSubscriptionStorage.get(ImmutablePair.of(deviceId, commandId));
            if (subscription == null || subscription.isDone()) { //Returns true if this task completed.
                // Completion may be due to normal termination, an exception, or cancellation --
                // in all of these cases, this method will return true.
                String path = "/device/" + deviceId + "/command/" + commandId + "/poll";
                UpdatesSubscriptionTask task = new UpdatesSubscriptionTask(hiveContext, path, Constants.WAIT_TIMEOUT);
                subscription = subscriptionExecutor.submit(task);
                commandsUpdateSubscriptionStorage.put(ImmutablePair.of(deviceId, commandId), subscription);
                logger.debug("New subscription added for device with id:" + deviceId + " and command id: " +
                        commandId);
            }
        } finally {
            rwCommandUpdateLock.writeLock().unlock();
        }
    }

    /**
     * Remove command subscription for following command name and device identifier. In case when no device identifiers specified,
     * surrogate subscription "for all available" will be removed. This subscription does not
     * include subscriptions for specific device.
     *
     * @param names     set of command names
     * @param deviceIds device identifiers.
     */
    public void removeCommandSubscription(Set<String> names, String... deviceIds) {
        unsubscribe(rwCommandsLock, commandsSubscriptionsStorage, names, deviceIds);
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
    public void addNotificationSubscription(Map<String, String> headers, Timestamp timestamp, Set<String> names,
                                            String... deviceIds) {
        if (deviceIds == null) {
            try {
                rwNotificationsLock.writeLock().lock();
                if (!notificationsSubscriptionsStorage
                        .containsKey(ImmutablePair.of(Constants.FOR_ALL_SUBSTITUTE, names))) {
                    String path = "/device/notification/poll";
                    SubscriptionTask task = new SubscriptionTask(hiveContext, timestamp, Constants.WAIT_TIMEOUT,
                            path, headers, names, Constants.FOR_ALL_SUBSTITUTE, DeviceNotification.class);
                    Future<Void> subscription = subscriptionExecutor.submit(task);
                    notificationsSubscriptionsStorage.put(ImmutablePair.of(Constants.FOR_ALL_SUBSTITUTE, names),
                            subscription);
                    logger.debug("New subscription added for:" + Constants.FOR_ALL_SUBSTITUTE);
                }
            } finally {
                rwNotificationsLock.writeLock().unlock();
            }
        } else {
            try {
                rwNotificationsLock.writeLock().lock();
                for (String id : deviceIds) {
                    Future<Void> subscription = notificationsSubscriptionsStorage.get(ImmutablePair.of(id, names));
                    if (subscription == null || subscription.isDone()) { //Returns true if this task completed.
                        // Completion may be due to normal termination, an exception, or cancellation --
                        // in all of these cases, this method will return true.
                        String path = "/device/" + id + "/notification/poll";
                        SubscriptionTask task = new SubscriptionTask(hiveContext, timestamp, Constants.WAIT_TIMEOUT,
                                path, headers, names, id, DeviceNotification.class);
                        subscription = subscriptionExecutor.submit(task);
                        notificationsSubscriptionsStorage.put(ImmutablePair.of(id, names), subscription);
                        logger.debug("New subscription added for device with id:" + id);
                    }
                }
            } finally {
                rwNotificationsLock.writeLock().unlock();
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
    public void removeNotificationSubscription(Set<String> names, String... deviceIds) {
        unsubscribe(rwNotificationsLock, notificationsSubscriptionsStorage, names, deviceIds);
    }

    private void unsubscribe(ReadWriteLock lock, Map<Pair<String, Set<String>>, Future<Void>> subscriptionStorage,
                             Set<String> names, String... deviceIds) {
        if (deviceIds == null) {
            try {
                lock.readLock().lock();
                Future<Void> task = subscriptionStorage.remove(ImmutablePair.of(Constants.FOR_ALL_SUBSTITUTE, names));
                if (task != null && !task.isDone()) {
                    boolean result = task.cancel(true);
                    logger.debug("Task is cancelled for device with id:" + Constants.FOR_ALL_SUBSTITUTE +
                            ". Cancellation result:" + result);
                }
            } finally {
                lock.readLock().unlock();
            }
        } else {
            try {
                lock.readLock().lock();
                for (String id : deviceIds) {
                    Future<Void> task = subscriptionStorage.remove(ImmutablePair.of(id, names));
                    if (task != null && !task.isDone()) {
                        boolean result = task.cancel(true);
                        logger.debug("Task is cancelled for device with id:" + id + ". Cancellation result:" + result);
                    }
                }
            } finally {
                lock.readLock().unlock();
            }
        }
    }

    public void addWsCommandsSubscription(Timestamp timestamp, Set<String> names, String... deviceIds) {
        rwWsCommandsLock.writeLock().lock();
        try {
            if (deviceIds == null) {
                deviceIds = new String[]{Constants.FOR_ALL_SUBSTITUTE};
            }
            for (String deviceId : deviceIds) {
                if (names != null) {
                    for (String name : names)
                        wsCommandSubscriptionsStorage.add(ImmutablePair.of(deviceId, name));
                    if (timestamp == null) {
                        timestamp = new Timestamp(System.currentTimeMillis());
                    }
                    if (!wsDeviceLastCommandTimestampAssociation.containsKey(deviceId))
                        wsDeviceLastCommandTimestampAssociation.put(deviceId, timestamp);
                } else {
                    wsCommandSubscriptionsStorage.add(ImmutablePair.<String, String>of(deviceId, null));
                }
            }
        } finally {
            rwWsCommandsLock.writeLock().unlock();
        }
    }

    public void addWsNotificationsSubscription(Timestamp timestamp, Set<String> names, String... deviceIds) {
        rwWsNotificationsLock.writeLock().lock();
        try {
            if (deviceIds == null) {
                deviceIds = new String[]{Constants.FOR_ALL_SUBSTITUTE};
            }
            for (String deviceId : deviceIds) {
                if (names != null) {
                    for (String name : names) {
                        wsNotificationSubscriptionsStorage.add(ImmutablePair.of(deviceId, name));
                        if (timestamp == null) {
                            timestamp = new Timestamp(System.currentTimeMillis());
                        }
                        if (!wsDeviceLastNotificationTimestampAssociation.containsKey(deviceId))
                            wsDeviceLastNotificationTimestampAssociation.put(deviceId, timestamp);
                    }
                } else {
                    wsNotificationSubscriptionsStorage.add(ImmutablePair.<String, String>of(deviceId, null));
                }
            }
        } finally {
            rwWsNotificationsLock.writeLock().unlock();
        }
    }

    public void addWsCommandUpdateSubscription(Long commandId, String deviceId) {
        rwWsCommandUpdateLock.writeLock().lock();
        try {
            wsCommandUpdateSubscriptionsStorage.put(commandId, deviceId);
        } finally {
            rwWsCommandUpdateLock.writeLock().unlock();
        }
    }

    public void removeWsCommandSubscription(Set<String> names, String... deviceIds) {
        rwWsCommandsLock.readLock().lock();
        try {
            if (deviceIds == null) {
                deviceIds = new String[]{Constants.FOR_ALL_SUBSTITUTE};
            }
            for (String deviceId : deviceIds)
                for (String name : names)
                    wsCommandSubscriptionsStorage.remove(ImmutablePair.of(deviceId, name));
        } finally {
            rwWsCommandsLock.readLock().unlock();
        }
    }

    public void removeWsNotificationSubscription(Set<String> names, String... deviceIds) {
        rwWsNotificationsLock.readLock().lock();
        try {
            if (deviceIds == null) {
                deviceIds = new String[]{Constants.FOR_ALL_SUBSTITUTE};
            }
            for (String deviceId : deviceIds)
                for (String name : names)
                    wsNotificationSubscriptionsStorage.remove(ImmutablePair.of(deviceId, name));
        } finally {
            rwWsNotificationsLock.readLock().unlock();
        }
    }

    public void removeWsCommandUpdateSubscription(Long commandId) {
        rwWsCommandUpdateLock.readLock().lock();
        try {
            wsCommandUpdateSubscriptionsStorage.remove(commandId);
        } finally {
            rwWsCommandUpdateLock.readLock().unlock();
        }
    }

    public void updateWsDeviceLastCommandTimestampAssociation(String deviceId, Timestamp newTimestamp) {
        rwWsCommandsLock.writeLock().lock();
        try {
            Timestamp lastTimestamp = wsDeviceLastCommandTimestampAssociation.get(deviceId);
            if (lastTimestamp != null && lastTimestamp.before(new Date(newTimestamp.getTime()))) {
                wsDeviceLastCommandTimestampAssociation.put(deviceId, newTimestamp);
            } else {
                wsDeviceLastCommandTimestampAssociation.put(deviceId, newTimestamp);
            }
        } finally {
            rwWsCommandsLock.writeLock().unlock();
        }
    }

    public void updateWsDeviceLastNotificationTimestampAssociation(String deviceId, Timestamp newTimestamp) {
        rwWsNotificationsLock.writeLock().lock();
        try {
            Timestamp lastTimestamp = wsDeviceLastNotificationTimestampAssociation.get(deviceId);
            if (lastTimestamp != null && lastTimestamp.before(new Date(newTimestamp.getTime()))) {
                wsDeviceLastNotificationTimestampAssociation.put(deviceId, newTimestamp);
            } else {
                wsDeviceLastNotificationTimestampAssociation.put(deviceId, newTimestamp);
            }
        } finally {
            rwWsNotificationsLock.writeLock().unlock();
        }
    }

    public void resubscribeForCommands() {
        rwWsCommandsLock.writeLock().lock();
        try {
            if (hiveContext.getHivePrincipal().getUser() != null || hiveContext.getHivePrincipal().getAccessKey() !=
                    null)
                for (Pair<String, String> currentSubscription : wsCommandSubscriptionsStorage) {
                    Timestamp timestamp = wsDeviceLastCommandTimestampAssociation.get(currentSubscription.getKey());
                    Set<String> names = new HashSet<>();
                    names.add(currentSubscription.getValue());
                    SubscriptionsService.subscribeClientForCommands(hiveContext, timestamp, names,
                            currentSubscription.getKey());
                }
            else if (hiveContext.getHivePrincipal().getDevice() != null) {
                for (Pair<String, String> currentSubscription : wsCommandSubscriptionsStorage) {
                    Timestamp timestamp = wsDeviceLastCommandTimestampAssociation.get(currentSubscription.getKey());
                    SubscriptionsService.subscribeDeviceForCommands(hiveContext, timestamp);
                }
            } //TODO gateway
        } finally {
            rwWsCommandsLock.writeLock().unlock();
        }
    }

    public void resubscribeForNotifications() {
        rwWsNotificationsLock.writeLock().lock();
        try {
            if (hiveContext.getHivePrincipal().getUser() != null ||
                    hiveContext.getHivePrincipal().getAccessKey() != null)
                for (Pair<String, String> currentSubscription : wsNotificationSubscriptionsStorage) {
                    Timestamp timestamp =
                            wsDeviceLastNotificationTimestampAssociation.get(currentSubscription.getKey());
                    Set<String> names = new HashSet<>();
                    names.add(currentSubscription.getValue());
                    SubscriptionsService.subscribeClientForNotifications(hiveContext, timestamp, names,
                            currentSubscription.getKey());
                }
        } finally {
            rwWsNotificationsLock.writeLock().unlock();
        }
    }

    public void requestCommandsUpdates() {
        Iterator<Map.Entry<Long, String>> iterator = wsCommandUpdateSubscriptionsStorage.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, String> entry = iterator.next();
            String path = "/device/" + entry.getValue() + "/command/" + entry.getKey();
            DeviceCommand command = hiveContext.getHiveRestClient()
                    .execute(path, HttpMethod.GET, null, DeviceCommand.class, COMMAND_TO_DEVICE);
            if (command.getStatus() != null) {
                try {
                    hiveContext.getCommandUpdateQueue().put(command);
                    iterator.remove();
                } catch (InterruptedException e) {
                    logger.warn("Unable to proceed command update!");
                }
            }
        }
    }

    /**
     * Kills threads which monitoring commands, command updates and notifications.
     */
    public void shutdownThreads() {
        try {
            rwNotificationsLock.writeLock().lock();
            subscriptionExecutor.shutdown();
            try {
                if (!subscriptionExecutor.awaitTermination(AWAIT_TERMINATION_TIMEOUT, TimeUnit.SECONDS)) {
                    subscriptionExecutor.shutdownNow();
                    if (!subscriptionExecutor.awaitTermination(AWAIT_TERMINATION_TIMEOUT, TimeUnit.SECONDS))
                        logger.warn("Pool did not terminate");
                }
            } catch (InterruptedException ie) {
                logger.warn(ie.getMessage(), ie);
                subscriptionExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        } finally {
            rwNotificationsLock.writeLock().unlock();
        }
    }
}
