package com.devicehive.client.api.client;


import com.devicehive.client.model.DeviceNotification;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.Timestamp;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Client side controller for device notifications: <i>/device/{deviceGuid}/notification</i>.
 * See <a href="http://www.devicehive.com/restful#Reference/DeviceNotification">DeviceHive RESTful API:
 * DeviceNotification</a> for details.
 * Transport declared in the hive context will be used.
 */
public interface NotificationsController {
    /**
     * Queries device notifications.
     * See <a href="http://www.devicehive.com/restful#Reference/DeviceNotification/query">DeviceHive
     * RESTful API: DeviceNotification: query</a> for more details.
     *
     * @param deviceId         device identifier
     * @param start            start timestamp
     * @param end              end timestamp
     * @param notificationName notification name
     * @param sortOrder        Result list sort order. Available values are ASC and DESC.
     * @param sortField        Result list sort field. Available values are Timestamp (default) and Notification.
     * @param take             Number of records to take from the result list (default is 1000).
     * @param skip             Number of records to skip from the result list.
     * @return If successful, this method returns list of device notifications.
     */
    List<DeviceNotification> queryNotifications(String deviceId, Timestamp start, Timestamp end,
                                                String notificationName,
                                                String sortOrder, String sortField, Integer take, Integer skip,
                                                Integer gridInterval);

    /**
     * Insert and send notification to the client.
     * See <a href="http://www.devicehive.com/restful#Reference/DeviceNotification/insert">DeviceHive
     * RESTful API: DeviceNotification: insert</a> for more details.
     *
     * @param deviceId     device identifier
     * @param notification notification to be inserted
     */
    DeviceNotification insertNotification(String deviceId, DeviceNotification notification);

    /**
     * Get information about required notification.
     * See <a href="http://www.devicehive.com/restful#Reference/DeviceNotification/get">DeviceHive
     * RESTful API: DeviceNotification: get</a> for more details.
     *
     * @param deviceId       device identifier
     * @param notificationId notification identifier
     * @return requested device notification resource
     */
    DeviceNotification getNotification(String deviceId, long notificationId);

    /**
     * Subscribes client to notifications. RESTful poll/pollMany or websocket subscribe will be used.
     *
     * @param timestamp start timestamp
     * @param names     names of the notifications
     * @param deviceIds device identifiers
     */
    void subscribeForNotifications(Timestamp timestamp, Set<String> names, String... deviceIds);

    /**
     * Unsubscribes client from notifications. In case of websocket unsubscribe method will be used,
     * otherwise polling thread must be terminated.
     *
     * @param names     names of the notifications
     * @param deviceIds device identifiers
     */
    void unsubscribeFromNotification(Set<String> names, String... deviceIds);

    /**
     * Get notification queue.
     *
     * @return notifications queue
     */
    Queue<Pair<String, DeviceNotification>> getNotificationsQueue();
}
