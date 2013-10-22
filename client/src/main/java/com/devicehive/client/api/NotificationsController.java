package com.devicehive.client.api;


import com.devicehive.client.model.DeviceNotification;

import java.sql.Timestamp;
import java.util.List;

public interface NotificationsController {

    //device notifications block
    List<DeviceNotification> queryNotifications(String guid, Timestamp start, Timestamp end, String notificationName,
                                                String sortOrder, String sortField, Integer take, Integer skip);

    DeviceNotification insertNotification(String guid, DeviceNotification notification);

    DeviceNotification getNotification(String guid, long notificationId);

    void subscribeForNotifications();

    void unsubscribeFromNotification();
}
