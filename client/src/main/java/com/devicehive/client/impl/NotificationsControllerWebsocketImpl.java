package com.devicehive.client.impl;


import com.devicehive.client.HiveMessageHandler;
import com.devicehive.client.impl.context.RestAgent;
import com.devicehive.client.impl.context.WebsocketAgent;
import com.devicehive.client.impl.json.GsonFactory;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.SubscriptionFilter;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.devicehive.client.model.exceptions.HiveException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_FROM_DEVICE;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_TO_DEVICE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

class NotificationsControllerWebsocketImpl extends NotificationsControllerRestImpl {

    private static final Logger logger = LoggerFactory.getLogger(NotificationsControllerWebsocketImpl.class);
    private final WebsocketAgent websocketAgent;

    NotificationsControllerWebsocketImpl(WebsocketAgent websocketAgent) {
        super(websocketAgent);
        this.websocketAgent = websocketAgent;
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
        result = websocketAgent.getWebsocketConnector().sendMessage(request, "notification",
                DeviceNotification.class, NOTIFICATION_TO_DEVICE);

        logger.debug("DeviceNotification: insert request proceed for device with id {} and notification name {} and " +
                "params {}. Result id {} and timestamp {}", guid, notification.getNotification(),
                notification.getParameters(), result.getId(), result.getTimestamp());
        return result;
    }


    @Override
    public String subscribeForNotifications(SubscriptionFilter filter,
                                          HiveMessageHandler<DeviceNotification> notificationsHandler)
            throws HiveException {
        logger.debug("Client: notification/subscribe requested for filter {},", filter);

      String subId =  websocketAgent.addNotificationsSubscription(filter, notificationsHandler);

        logger.debug("Client: notification/subscribe proceed for filter {},", filter);
        return subId;
    }

    @Override
    public void unsubscribeFromNotification(String subscriptionId) throws HiveException {
        logger.debug("Client: notification/unsubscribe requested.");
        websocketAgent.removeNotificationsSubscription(subscriptionId);
        logger.debug("Client: notification/unsubscribe proceed.");
    }

}
