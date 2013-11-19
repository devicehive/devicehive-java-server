package com.devicehive.client.api.client;


import com.devicehive.client.model.DeviceNotification;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.Timestamp;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public interface NotificationsController {

    //device notifications block
    List<DeviceNotification> queryNotifications(String guid, Timestamp start, Timestamp end, String notificationName,
                                                String sortOrder, String sortField, Integer take, Integer skip);

    DeviceNotification insertNotification(String guid, DeviceNotification notification);

    DeviceNotification getNotification(String guid, long notificationId);

    void subscribeForNotifications(Timestamp timestamp, Set<String> names, String ... deviceIds);

    void unsubscribeFromNotification(Set<String> names, String ... deviceIds);

    Queue<Pair<String, DeviceNotification>> getNotificationsQueue();
}
