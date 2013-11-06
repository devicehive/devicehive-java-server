package com.devicehive.client.api;


import com.devicehive.client.context.HiveContext;
import com.devicehive.client.model.DeviceNotification;
import com.google.common.reflect.TypeToken;

import javax.ws.rs.HttpMethod;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.*;
public class NotificationsControllerImpl implements NotificationsController{

    private final HiveContext hiveContext;

    public NotificationsControllerImpl(HiveContext hiveContext) {
        this.hiveContext = hiveContext;
    }

    @Override
    public List<DeviceNotification> queryNotifications(String guid, Timestamp start, Timestamp end,
                                                       String notificationName, String sortOrder, String sortField,
                                                       Integer take, Integer skip) {
        String path = "/device/" + guid + "/notification";
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("start", start);
        queryParams.put("end", end);
        queryParams.put("notification", notificationName);
        queryParams.put("sortField", sortField);
        queryParams.put("sortOrder", sortOrder);
        queryParams.put("take", take);
        queryParams.put("skip", skip);
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, queryParams,
                new TypeToken<List<DeviceNotification>>() {
                }.getType(), NOTIFICATION_TO_CLIENT);
    }

    @Override
    public DeviceNotification insertNotification(String guid, DeviceNotification notification) {
        String path = "/device/" + guid + "/notification";
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, null, null, notification,
                DeviceNotification.class, NOTIFICATION_FROM_DEVICE, NOTIFICATION_TO_DEVICE);
    }

    @Override
    public DeviceNotification getNotification(String guid, long notificationId) {
        String path = "/device/" + guid + "/command/" + notificationId;
        return hiveContext.getHiveRestClient()
                .execute(path, HttpMethod.GET, null, DeviceNotification.class, NOTIFICATION_TO_CLIENT);
    }

    @Override
    public void subscribeForNotifications(Timestamp timestamp, Set<String> names, String ... deviceIds) {
        hiveContext.getHiveSubscriptions().addNotificationSubscription(null, timestamp, names, deviceIds);
    }

    @Override
    public void unsubscribeFromNotification(Set<String> names,String ... deviceIds) {
        hiveContext.getHiveSubscriptions().removeCommandSubscription(names, deviceIds);
    }
}
