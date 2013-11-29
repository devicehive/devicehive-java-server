package com.devicehive.client.api.client;


import com.devicehive.client.api.SubscriptionsService;
import com.devicehive.client.context.HiveContext;
import com.devicehive.client.json.GsonFactory;
import com.devicehive.client.json.adapters.TimestampAdapter;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.sql.Timestamp;
import java.util.*;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.*;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

public class NotificationsControllerImpl implements NotificationsController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationsControllerImpl.class);
    private final HiveContext hiveContext;

    public NotificationsControllerImpl(HiveContext hiveContext) {
        this.hiveContext = hiveContext;
    }

    @Override
    public List<DeviceNotification> queryNotifications(String guid, Timestamp start, Timestamp end,
                                                       String notificationName, String sortOrder, String sortField,
                                                       Integer take, Integer skip, Integer gridInterval) {
        logger.debug("DeviceNotification: query requested with parameters: device id {}, start timestamp {}, " +
                "end timestamp {}, notification name {}, sort order {}, sort field {}, take {}, skip {}, " +
                "grid interval {}", guid, start, end, notificationName, sortOrder, sortField, take, skip, gridInterval);
        String path = "/device/" + guid + "/notification";
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("start", TimestampAdapter.formatTimestamp(start));
        queryParams.put("end", TimestampAdapter.formatTimestamp(end));
        queryParams.put("notification", notificationName);
        queryParams.put("sortField", sortField);
        queryParams.put("sortOrder", sortOrder);
        queryParams.put("take", take);
        queryParams.put("skip", skip);
        queryParams.put("gridInterval", gridInterval);
        List<DeviceNotification> result = hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null,
                queryParams, new TypeToken<List<DeviceNotification>>() {
        }.getType(), NOTIFICATION_TO_CLIENT);
        logger.debug("DeviceNotification: query request proceed with parameters: device id {}, start timestamp {}, " +
                "end timestamp {}, notification name {}, sort order {}, sort field {}, take {}, skip {}, " +
                "grid interval {}", guid, start, end, notificationName, sortOrder, sortField, take, skip, gridInterval);
        return result;
    }

    @Override
    public DeviceNotification insertNotification(String guid, DeviceNotification notification) {
        if (notification == null) {
            throw new HiveClientException("Notification cannot be null!", BAD_REQUEST.getStatusCode());
        }
        logger.debug("DeviceNotification: insert requested for device with id {} and notification name {} and params " +
                "{}", guid, notification.getNotification(), notification.getParameters());
        DeviceNotification result;
        if (hiveContext.useSockets()) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "notification/insert");
            String requestId = UUID.randomUUID().toString();
            request.addProperty("requestId", requestId);
            request.addProperty("deviceGuid", guid);
            Gson gson = GsonFactory.createGson(NOTIFICATION_FROM_DEVICE);
            request.add("notification", gson.toJsonTree(notification));
            result = hiveContext.getHiveWebSocketClient().sendMessage(request, "notification",
                    DeviceNotification.class, NOTIFICATION_TO_DEVICE);
        } else {
            String path = "/device/" + guid + "/notification";
            result = hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, null, null, notification,
                    DeviceNotification.class, NOTIFICATION_FROM_DEVICE, NOTIFICATION_TO_DEVICE);
        }
        logger.debug("DeviceNotification: insert request proceed for device with id {} and notification name {} and " +
                "params {}. Result id {} and timestamp {}", guid, notification.getNotification(),
                notification.getParameters(), result.getId(), result.getTimestamp());
        return result;
    }

    @Override
    public DeviceNotification getNotification(String guid, long notificationId) {
        logger.debug("DeviceNotification: get requested for device with id {} and notification id {}", guid,
                notificationId);
        String path = "/device/" + guid + "/notification/" + notificationId;
        DeviceNotification result = hiveContext.getHiveRestClient()
                .execute(path, HttpMethod.GET, null, DeviceNotification.class, NOTIFICATION_TO_CLIENT);
        logger.debug("DeviceNotification: get request proceed for device with id {} and notification id {}", guid,
                notificationId);
        return result;
    }

    @Override
    public void subscribeForNotifications(Timestamp timestamp, Set<String> names, String... deviceIds) {
        logger.debug("Client: notification/subscribe requested. Params: timestamp {}, names {}, device ids {}",
                timestamp, names, deviceIds);
        if (hiveContext.useSockets()) {
            SubscriptionsService.subscribeClientForNotifications(hiveContext, timestamp, names, deviceIds);
        } else {
            hiveContext.getHiveSubscriptions().addNotificationSubscription(null, timestamp, names, deviceIds);
        }
        logger.debug("Client: notification/subscribe proceed. Params: timestamp {}, names {}, device ids {}",
                timestamp, names, deviceIds);
    }

    @Override
    public void unsubscribeFromNotification(Set<String> names, String... deviceIds) {
        logger.debug("Client: notification/unsubscribe requested. Params: names {}, device ids {}", names, deviceIds);
        if (hiveContext.useSockets()) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "notification/unsubscribe");
            String requestId = UUID.randomUUID().toString();
            request.addProperty("requestId", requestId);
            Gson gson = GsonFactory.createGson();
            request.add("deviceGuids", gson.toJsonTree(deviceIds));
            hiveContext.getHiveWebSocketClient().sendMessage(request);
            hiveContext.getHiveSubscriptions().removeWsNotificationSubscription(names, deviceIds);
        } else {
            hiveContext.getHiveSubscriptions().removeNotificationSubscription(names, deviceIds);
        }
        logger.debug("Client: notification/unsubscribe proceed. Params: names {}, device ids {}", names, deviceIds);
    }

    public Queue<Pair<String, DeviceNotification>> getNotificationsQueue() {
        return hiveContext.getNotificationQueue();
    }
}
