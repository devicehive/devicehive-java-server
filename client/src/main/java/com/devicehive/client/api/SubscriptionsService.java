package com.devicehive.client.api;


import com.devicehive.client.context.HiveContext;
import com.devicehive.client.json.GsonFactory;
import com.devicehive.client.json.adapters.TimestampAdapter;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.Timestamp;
import java.util.Set;
import java.util.UUID;

public class SubscriptionsService {

    public static void subscribeClientForCommands(HiveContext hiveContext, Timestamp timestamp, Set<String> names,
                                                  String... deviceIds) {
        JsonObject request = new JsonObject();
        request.addProperty("action", "command/subscribe");
        String requestId = UUID.randomUUID().toString();
        request.addProperty("requestId", requestId);
        request.addProperty("timestamp", TimestampAdapter.formatTimestamp(timestamp));
        Gson gson = GsonFactory.createGson();
        request.add("deviceGuids", gson.toJsonTree(deviceIds));
        request.add("names", gson.toJsonTree(names));
        hiveContext.getHiveWebSocketClient().sendMessage(request);
        hiveContext.getHiveSubscriptions().addWsCommandsSubscription(timestamp, names, deviceIds);
    }

    public static void subscribeClientForNotifications(HiveContext hiveContext, Timestamp timestamp,
                                                       Set<String> names, String... deviceIds) {
        JsonObject request = new JsonObject();
        request.addProperty("action", "notification/subscribe");
        String requestId = UUID.randomUUID().toString();
        request.addProperty("requestId", requestId);
        request.addProperty("timestamp", TimestampAdapter.formatTimestamp(timestamp));
        Gson gson = GsonFactory.createGson();
        request.add("deviceGuids", gson.toJsonTree(deviceIds));
        request.add("names", gson.toJsonTree(names));
        hiveContext.getHiveWebSocketClient().sendMessage(request);
        hiveContext.getHiveSubscriptions().addWsNotificationsSubscription(timestamp, names, deviceIds);
    }

    public static void subscribeDeviceForCommands(HiveContext hiveContext, Timestamp timestamp) {
        JsonObject request = new JsonObject();
        request.addProperty("action", "command/subscribe");
        String requestId = UUID.randomUUID().toString();
        request.addProperty("requestId", requestId);
        request.addProperty("timestamp", TimestampAdapter.formatTimestamp(timestamp));
        hiveContext.getHiveWebSocketClient().sendMessage(request);
        Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
        hiveContext.getHiveSubscriptions().addWsCommandsSubscription(timestamp, null, authenticated.getLeft());
    }

    public static void subscribeDeviceForCommands(HiveContext hiveContext, Timestamp timestamp, String deviceId,
                                                  String key) {
        JsonObject request = new JsonObject();
        request.addProperty("action", "command/subscribe");
        String requestId = UUID.randomUUID().toString();
        request.addProperty("requestId", requestId);
        request.addProperty("deviceId", deviceId);
        request.addProperty("deviceKey", key);
        request.addProperty("timestamp", TimestampAdapter.formatTimestamp(timestamp));
        hiveContext.getHiveWebSocketClient().sendMessage(request);
        hiveContext.getHiveSubscriptions().addWsCommandsSubscription(timestamp, null, deviceId);
    }
}
