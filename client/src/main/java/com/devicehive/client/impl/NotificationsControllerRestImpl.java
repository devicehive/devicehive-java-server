package com.devicehive.client.impl;


import com.devicehive.client.NotificationsController;
import com.devicehive.client.impl.context.HiveContext;
import com.devicehive.client.impl.json.adapters.TimestampAdapter;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.devicehive.client.model.exceptions.HiveException;
import com.google.common.reflect.TypeToken;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.sql.Timestamp;
import java.util.*;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.*;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

class NotificationsControllerRestImpl implements NotificationsController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationsControllerRestImpl.class);
    protected final HiveContext hiveContext;

    public NotificationsControllerRestImpl(HiveContext hiveContext) {
        this.hiveContext = hiveContext;
    }

    @SuppressWarnings("serial")
    @Override
    public List<DeviceNotification> queryNotifications(String guid, Timestamp start, Timestamp end,
                                                       String notificationName, String sortOrder, String sortField,
                                                       Integer take, Integer skip, Integer gridInterval)
            throws HiveException {
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
    public DeviceNotification insertNotification(String guid, DeviceNotification notification)
            throws HiveException {
        if (notification == null) {
            throw new HiveClientException("Notification cannot be null!", BAD_REQUEST.getStatusCode());
        }
        logger.debug("DeviceNotification: insert requested for device with id {} and notification name {} and params " +
                "{}", guid, notification.getNotification(), notification.getParameters());
        DeviceNotification result;
        String path = "/device/" + guid + "/notification";
        result = hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, null, null, notification,
                DeviceNotification.class, NOTIFICATION_FROM_DEVICE, NOTIFICATION_TO_DEVICE);
        logger.debug("DeviceNotification: insert request proceed for device with id {} and notification name {} and " +
                "params {}. Result id {} and timestamp {}", guid, notification.getNotification(),
                notification.getParameters(), result.getId(), result.getTimestamp());
        return result;
    }

    @Override
    public DeviceNotification getNotification(String guid, long notificationId) throws HiveException {
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
    public void subscribeForNotifications(Timestamp timestamp, Set<String> names, String... deviceIds)
            throws HiveException {
        logger.debug("Client: notification/subscribe requested. Params: timestamp {}, names {}, device ids {}",
                timestamp, names, deviceIds);

        hiveContext.getRestSubManager().addNotificationSubscription(null, timestamp, names, deviceIds);

        logger.debug("Client: notification/subscribe proceed. Params: timestamp {}, names {}, device ids {}",
                timestamp, names, deviceIds);
    }

    @Override
    public void unsubscribeFromNotification(Set<String> names, String... deviceIds) throws HiveException {
        logger.debug("Client: notification/unsubscribe requested. Params: names {}, device ids {}", names, deviceIds);

        hiveContext.getRestSubManager().removeNotificationSubscription(names, deviceIds);

        logger.debug("Client: notification/unsubscribe proceed. Params: names {}, device ids {}", names, deviceIds);
    }

    public Queue<Pair<String, DeviceNotification>> getNotificationsQueue() {
        return hiveContext.getNotificationQueue();
    }
}
