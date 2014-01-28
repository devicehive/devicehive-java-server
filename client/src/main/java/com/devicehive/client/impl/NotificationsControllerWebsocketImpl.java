package com.devicehive.client.impl;


import com.devicehive.client.impl.context.HiveContext;
import com.devicehive.client.impl.json.GsonFactory;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.devicehive.client.model.exceptions.HiveException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.*;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.*;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

class NotificationsControllerWebsocketImpl extends NotificationsControllerRestImpl {

    private static final Logger logger = LoggerFactory.getLogger(NotificationsControllerWebsocketImpl.class);

    public NotificationsControllerWebsocketImpl(HiveContext hiveContext) {
        super(hiveContext);
    }


    @Override
    public DeviceNotification insertNotification(String guid, DeviceNotification notification) throws HiveException {
        if (notification == null) {
            throw new HiveClientException("Notification cannot be null!", BAD_REQUEST.getStatusCode());
        }
        logger.debug("DeviceNotification: insert requested for device with id {} and notification name {} and params " +
                "{}", guid, notification.getNotification(), notification.getParameters());
        DeviceNotification result;
        JsonObject request = new JsonObject();
        request.addProperty("action", "notification/insert");
        String requestId = UUID.randomUUID().toString();
        request.addProperty("requestId", requestId);
        request.addProperty("deviceGuid", guid);
        Gson gson = GsonFactory.createGson(NOTIFICATION_FROM_DEVICE);
        request.add("notification", gson.toJsonTree(notification));
        result = hiveContext.getHiveWebSocketClient().sendMessage(request, "notification",
                DeviceNotification.class, NOTIFICATION_TO_DEVICE);

        logger.debug("DeviceNotification: insert request proceed for device with id {} and notification name {} and " +
                "params {}. Result id {} and timestamp {}", guid, notification.getNotification(),
                notification.getParameters(), result.getId(), result.getTimestamp());
        return result;
    }


    @Override
    public void subscribeForNotifications(Timestamp timestamp, Set<String> names, String... deviceIds)
            throws HiveException {
        logger.debug("Client: notification/subscribe requested. Params: timestamp {}, names {}, device ids {}",
                timestamp, names, deviceIds);
        hiveContext.getWebsocketSubManager().addNotificationSubscription(timestamp, names, deviceIds);
        logger.debug("Client: notification/subscribe proceed. Params: timestamp {}, names {}, device ids {}",
                timestamp, names, deviceIds);
    }

    @Override
    public void unsubscribeFromNotification(Set<String> names, String... deviceIds) throws HiveException {
        logger.debug("Client: notification/unsubscribe requested. Params: names {}, device ids {}", names, deviceIds);
        hiveContext.getWebsocketSubManager().removeNotificationSubscription(names, deviceIds);
        logger.debug("Client: notification/unsubscribe proceed. Params: names {}, device ids {}", names, deviceIds);
    }

}
