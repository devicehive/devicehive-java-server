package com.devicehive.client.impl.websocket;


import com.devicehive.client.impl.context.HiveContext;
import com.devicehive.client.impl.json.GsonFactory;
import com.devicehive.client.impl.json.adapters.TimestampAdapter;
import com.devicehive.client.impl.rest.subs.SubManager;
import com.devicehive.client.model.exceptions.HiveException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Set;

public class WebsocketSubManager implements AutoCloseable, SubManager {

    private static Logger logger = LoggerFactory.getLogger(WebsocketSubManager.class);

    private final HiveContext hiveContext;

    public WebsocketSubManager(HiveContext hiveContext) {
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
    @Override
    public synchronized void addCommandsSubscription(Map<String, String> headers, Timestamp timestamp,
                                                     Set<String> names, String... deviceIds) throws HiveException {
        JsonObject request = new JsonObject();
        request.addProperty("action", "command/subscribe");
        request.addProperty("timestamp", TimestampAdapter.formatTimestamp(timestamp));
        Gson gson = GsonFactory.createGson();
        request.add("deviceGuids", gson.toJsonTree(deviceIds));
        request.add("names", gson.toJsonTree(names));
        hiveContext.getHiveWebSocketClient().sendMessage(request);
    }

    /**
     * Put command updates into the queue as soon as update coming. Command update subscription adds when the command
     * insert executes.
     *
     * @param commandId command identifier
     * @param deviceId  device identifier
     */
    @Override
    public synchronized void addCommandUpdateSubscription(long commandId, String deviceId) {
        //nothing to do - this subscription is added implicitly on command send
    }

    /**
     * Remove command subscription for following command name and device identifier. In case when no device identifiers specified,
     * surrogate subscription "for all available" will be removed. This subscription does not
     * include subscriptions for specific device.
     *
     * @param names     set of command names
     * @param deviceIds device identifiers.
     */
    @Override
    public synchronized void removeCommandSubscription(Set<String> names, String... deviceIds) throws HiveException {
        JsonObject request = new JsonObject();
        request.addProperty("action", "command/unsubscribe");
        Gson gson = GsonFactory.createGson();
        request.add("deviceGuids", gson.toJsonTree(deviceIds));
        request.add("names", gson.toJsonTree(names));
        hiveContext.getHiveWebSocketClient().sendMessage(request);
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
    @Override
    public synchronized void addNotificationSubscription(Map<String, String> headers, Timestamp timestamp, Set<String> names,
                                                         String... deviceIds) throws HiveException {
        JsonObject request = new JsonObject();
        request.addProperty("action", "notification/subscribe");
        request.addProperty("timestamp", TimestampAdapter.formatTimestamp(timestamp));
        Gson gson = GsonFactory.createGson();
        request.add("deviceGuids", gson.toJsonTree(deviceIds));
        request.add("names", gson.toJsonTree(names));
        hiveContext.getHiveWebSocketClient().sendMessage(request);
    }

    /**
     * Remove notification subscription for following notification name and device identifier. In case when no device
     * identifiers specified, surrogate subscription "for all available" will be removed. This subscription does not
     * include subscriptions for specific device.
     *
     * @param names     set of notification names
     * @param deviceIds device identifiers.
     */
    @Override
    public synchronized void removeNotificationSubscription(Set<String> names, String... deviceIds) throws HiveException {
        JsonObject request = new JsonObject();
        request.addProperty("action", "notification/unsubscribe");
        Gson gson = GsonFactory.createGson();
        request.add("deviceGuids", gson.toJsonTree(deviceIds));
        request.add("names", gson.toJsonTree(names));
        hiveContext.getHiveWebSocketClient().sendMessage(request);
    }

    @Override
    public synchronized void resubscribeAll() {
        //TODO implement subscriptions restore
    }

    @Override
    public void close() {
        //nothing to do
    }
}
