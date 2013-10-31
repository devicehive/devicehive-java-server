package com.devicehive.client.api;


import com.devicehive.client.config.Constants;
import com.devicehive.client.context.HiveContext;
import com.devicehive.client.json.adapters.TimestampAdapter;
import com.devicehive.client.model.*;
import com.devicehive.client.util.HiveValidator;
import com.google.common.reflect.TypeToken;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.log4j.Logger;

import javax.ws.rs.HttpMethod;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.*;

public class HiveDeviceGateway implements Closeable {
    private static final int SUBSCRIPTIONS_THREAD_POOL_SIZE = 100;
    private static Logger logger = Logger.getLogger(HiveDeviceGateway.class);
    private HiveContext hiveContext;
    private ExecutorService subscriptionExecutor = Executors.newFixedThreadPool(SUBSCRIPTIONS_THREAD_POOL_SIZE);
    private Map<String, Future<Void>> deviceSubscriptionsStorage = new HashMap<>();
    private ReadWriteLock rwLock = new ReentrantReadWriteLock();


    public HiveDeviceGateway(URI restUri) {
        hiveContext = new HiveContext(Transport.AUTO, restUri);
    }

    public static void main(String... args) {
        HiveDeviceGateway gateway = new HiveDeviceGateway(URI.create("http://127.0.0.1:8080/hive/rest/"));
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
            Date startDate = formatter.parse("2013-10-11 13:12:00");
            gateway.subscribeForCommands("e50d6085-2aba-48e9-b1c3-73c673e414be", "05F94BF509C8",
                    new Timestamp(startDate.getTime()), 40);
        } catch (ParseException e) {
            logger.error(e);
        }
    }

    @Override
    public void close() throws IOException {
        //should we unsubscribe device on device gateway close?

    }

    public Device getDevice(String deviceId, String key) {
        Map<String, String> headers = getHeaders(deviceId, key);
        String path = "/device/" + deviceId;
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, headers, Device.class, null);
    }

    public void saveDevice(String deviceId, String key, Device device) {
        Map<String, String> headers = getHeaders(deviceId, key);
        device.setId(deviceId);
        device.setKey(key);
        HiveValidator.validate(device);
        String path = "/device/" + device.getId();
        hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, headers, device, null);

    }

    public List<DeviceCommand> queryCommands(String deviceId, String key, Timestamp start, Timestamp end,
                                             String command, String status, String sortBy, boolean sortAsc,
                                             Integer take, Integer skip) {
        String path = "/device/" + deviceId + "/command";
        Map<String, String> headers = getHeaders(deviceId, key);
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("start", start);
        queryParams.put("end", end);
        queryParams.put("command", command);
        queryParams.put("status", status);
        queryParams.put("sortField", sortBy);
        String order = sortAsc ? "ASC" : "DESC";
        queryParams.put("sortOrder", order);
        queryParams.put("take", take);
        queryParams.put("skip", skip);
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, headers, queryParams,
                new TypeToken<List<DeviceCommand>>() {
                }.getType(), null);
    }

    public DeviceCommand getCommand(String deviceId, String key, long commandId) {
        Map<String, String> headers = getHeaders(deviceId, key);
        String path = "/device/" + deviceId + "/command/" + commandId;
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, headers, DeviceCommand.class, null);
    }

    public void updateCommand(String deviceId, String key, DeviceCommand deviceCommand) {
        Map<String, String> headers = getHeaders(deviceId, key);
        String path = "/device/" + key + "/command/" + deviceCommand.getId();
        hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, headers, deviceCommand,
                COMMAND_UPDATE_FROM_DEVICE);

    }

    public void subscribeForCommands(String deviceId, String key, final Timestamp timestamp,
                                     final Integer waitTimeout) {
        try {
            rwLock.writeLock().lock();
            final Map<String, String> headers = getHeaders(deviceId, key);
            Future<Void> existingSubscriptionTask = deviceSubscriptionsStorage.get(deviceId);
            if (existingSubscriptionTask == null || existingSubscriptionTask.isCancelled() ||
                    existingSubscriptionTask.isDone()) {
                final String path = "/device/" + deviceId + "/command/poll";
                Future<Void> execResult = subscriptionExecutor.submit(new SubscriptionTask(hiveContext, timestamp,
                        waitTimeout, path, headers));
                deviceSubscriptionsStorage.put(deviceId, execResult);
            }
        } finally {
            rwLock.writeLock().unlock();
        }

    }

    public void unsubscribeFromCommands(String deviceId, String key) {
        try {
            rwLock.readLock().lock();
            Future<Void> existingTask = deviceSubscriptionsStorage.get(deviceId);
            if (existingTask != null && !existingTask.isCancelled() && !existingTask.isDone()) {
                existingTask.cancel(true);
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public DeviceNotification insertNotification(String deviceId, String key, DeviceNotification deviceNotification) {
        Map<String, String> headers = getHeaders(deviceId, key);
        HiveValidator.validate(deviceNotification);
        String path = "/device/" + deviceId + "notification";
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, headers, null, deviceNotification,
                DeviceNotification.class, NOTIFICATION_FROM_DEVICE, NOTIFICATION_TO_DEVICE);

    }

    private Map<String, String> getHeaders(String deviceId, String key) {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.DEVICE_ID_HEADER, deviceId);
        headers.put(Constants.DEVICE_KEY_HEADER, key);
        return headers;
    }

    private class SubscriptionTask implements Callable<Void> {

        private final HiveContext hiveContext;
        private final Timestamp timestamp;
        private final Integer waitTimeout;
        private final String path;
        private final Map<String, String> headers;

        public SubscriptionTask(HiveContext hiveContext, Timestamp timestamp, Integer waitTimeout, String path,
                                Map<String, String> headers) {
            this.hiveContext = ObjectUtils.cloneIfPossible(hiveContext);
            this.timestamp = timestamp;
            this.waitTimeout = waitTimeout;
            this.path = path;
            this.headers = headers;
        }

        @Override
        public Void call() {
            try {
                while (true) {
                    Map<String, Object> queryParams = new HashMap<>();
                    queryParams.put("timestamp", TimestampAdapter.formatTimestamp(timestamp));
                    queryParams.put("waitTimeout", waitTimeout);
                    List<DeviceCommand> returned =
                            hiveContext.getHiveRestClient().executeAsync(path, HttpMethod.GET, headers,
                                    queryParams, null, new TypeToken<List<DeviceCommand>>() {
                            }.getType(), null, COMMAND_LISTED);
                    System.out.println("\n----Start Timestamp: " + timestamp + "----");
                    for (DeviceCommand current : returned) {
                        System.out.println("id: " + current.getId() + "timestamp:" + current.getTimestamp());
                    }
                    if (!returned.isEmpty()) {
                        hiveContext.getCommandQueue().addAll(returned);
                        timestamp.setTime(returned.get(returned.size() - 1).getTimestamp().getTime());
                    }
                }
            } catch (Exception e) {
                logger.error(e);
            }
            return null;
        }
    }

    public ApiInfo getInfo() {
        return hiveContext.getInfo();
    }

}
