package com.devicehive.client.context;


import com.devicehive.client.config.Constants;
import com.devicehive.client.model.ApiInfo;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.Transport;
import com.devicehive.client.util.SubscriptionTask;
import com.devicehive.client.util.UpdatesSubscriptionTask;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class HiveContext implements Closeable {

    private static final int SUBSCRIPTIONS_THREAD_POOL_SIZE = 100;
    private static Logger logger = Logger.getLogger(HiveContext.class);
    private final Transport transport;
    private HiveRestClient hiveRestClient;
    private HivePrincipal hivePrincipal;
    private ExecutorService subscriptionExecutor = Executors.newFixedThreadPool(SUBSCRIPTIONS_THREAD_POOL_SIZE);
    private Map<String, Future<Response>> websocketResponsesMap = new HashMap<>();
    private Map<Pair<String, Set<String>>, Future<Void>> commandsSubscriptionsStorage = new HashMap<>();
    private Map<Pair<String, Set<String>>, Future<Void>> notificationsSubscriptionsStorage = new HashMap<>();
    private Map<Pair<String, Long>, Future<DeviceCommand>> commandsUpdateSubscriptionStorage = new HashMap<>();
    private ReadWriteLock rwCommandsLock = new ReentrantReadWriteLock();
    private ReadWriteLock rwCommandUpdateLock = new ReentrantReadWriteLock();
    private ReadWriteLock rwNotificationsLock = new ReentrantReadWriteLock();
    private BlockingQueue<DeviceCommand> commandQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<DeviceCommand> commandUpdateQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<DeviceNotification> notificationQueue = new LinkedBlockingQueue<>();

    public HiveContext(Transport transport, URI rest) {
        this.transport = transport;
        hiveRestClient = new HiveRestClient(rest, this);
    }

    @Override
    public synchronized void close() throws IOException {
        try {
            shutdownThreads();
        } finally {
            hiveRestClient.close();
        }
    }

    public HiveRestClient getHiveRestClient() {
        return hiveRestClient;
    }

    public synchronized HivePrincipal getHivePrincipal() {
        return hivePrincipal;
    }

    public synchronized void setHivePrincipal(HivePrincipal hivePrincipal) {
        if (this.hivePrincipal != null) {
            throw new IllegalStateException("Principal is already set");
        }
        this.hivePrincipal = hivePrincipal;
    }

    public synchronized ApiInfo getInfo() {
        return hiveRestClient.execute("/info", HttpMethod.GET, null, ApiInfo.class, null);
    }

    public BlockingQueue<DeviceCommand> getCommandQueue() {
        return commandQueue;
    }

    public BlockingQueue<DeviceCommand> getCommandUpdateQueue() {
        return commandUpdateQueue;
    }

    public BlockingQueue<DeviceNotification> getNotificationQueue() {
        return notificationQueue;
    }

    public void addCommandsSubscription(Map<String, String> headers, Timestamp timestamp,
                                        Set<String> names, String... deviceIds) {
        if (deviceIds == null) {
            try {
                rwCommandsLock.writeLock().lock();
                if (!commandsSubscriptionsStorage.containsKey(ImmutablePair.of(Constants.FOR_ALL_SUBSTITUTE, names))) {
                    String path = "/device/command/poll";
                    SubscriptionTask task = new SubscriptionTask(this, timestamp, Constants.WAIT_TIMEOUT,
                            path, headers, names);
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
                        SubscriptionTask task = new SubscriptionTask(this, timestamp, Constants.WAIT_TIMEOUT,
                                path, headers, names);
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

    public void addCommandUpdateSubscription(long commandId, String deviceId) {
        try {
            rwCommandUpdateLock.writeLock().lock();
            Future<DeviceCommand> subscription =
                    commandsUpdateSubscriptionStorage.get(ImmutablePair.of(deviceId, commandId));
            if (subscription == null || subscription.isDone()) { //Returns true if this task completed.
                // Completion may be due to normal termination, an exception, or cancellation --
                // in all of these cases, this method will return true.
                String path = "/device/" + deviceId + "/command/" + commandId + "/poll";
                UpdatesSubscriptionTask task = new UpdatesSubscriptionTask(this, path, Constants.WAIT_TIMEOUT);
                subscription = subscriptionExecutor.submit(task);
                commandsUpdateSubscriptionStorage.put(ImmutablePair.of(deviceId, commandId), subscription);
                logger.debug("New subscription added for device with id:" + deviceId + " and command id: " +
                        commandId);
            }
        } finally {
            rwCommandUpdateLock.writeLock().unlock();
        }
    }

    public void removeCommandSubscription(Set<String> names, String... deviceIds) {
        unsubscribe(rwCommandsLock, commandsSubscriptionsStorage, names, deviceIds);
    }

    public void addNotificationSubscription(Map<String, String> headers, Timestamp timestamp, Set<String> names,
                                            String... deviceIds) {
        if (deviceIds == null) {
            try {
                rwCommandsLock.writeLock().lock();
                if (!commandsSubscriptionsStorage.containsKey(ImmutablePair.of(Constants.FOR_ALL_SUBSTITUTE, names))) {
                    String path = "/device/notification/poll";
                    SubscriptionTask task = new SubscriptionTask(this, timestamp, Constants.WAIT_TIMEOUT,
                            path, headers, names);
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
                        String path = "/device/" + id + "/notification/poll";
                        SubscriptionTask task = new SubscriptionTask(this, timestamp, Constants.WAIT_TIMEOUT,
                                path, headers, names);
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

    private void shutdownThreads() {
        try {
            rwNotificationsLock.writeLock().lock();
            subscriptionExecutor.shutdown();
            try {
                if (!subscriptionExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    subscriptionExecutor.shutdownNow();
                    if (!subscriptionExecutor.awaitTermination(10, TimeUnit.SECONDS))
                        logger.warn("Pool did not terminate");
                }
            } catch (InterruptedException ie) {
                logger.warn(ie);
                subscriptionExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        } finally {
            rwNotificationsLock.writeLock().unlock();
        }
    }


}
