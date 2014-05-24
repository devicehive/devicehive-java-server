package com.devicehive.client.impl.context;

import com.devicehive.client.HiveMessageHandler;
import com.devicehive.client.impl.json.GsonFactory;
import com.devicehive.client.impl.util.Messages;
import com.devicehive.client.impl.context.connection.HiveConnectionEventHandler;
import com.devicehive.client.model.ApiInfo;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.SubscriptionFilter;
import com.devicehive.client.model.exceptions.HiveException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.UUID;


public class HiveWebsocketContext extends HiveRestContext {

    private static Logger logger = LoggerFactory.getLogger(HiveRestConnector.class);
    private HiveWebsocketConnector websocketConnector;

    public HiveWebsocketContext(URI restUri,
                                HiveMessageHandler<DeviceCommand> commandUpdatesHandler,
                                HiveConnectionEventHandler connectionEventHandler) throws HiveException {
        super(restUri, commandUpdatesHandler, connectionEventHandler);
        URI wsUri = URI.create(super.getInfo().getWebSocketServerUrl());
        this.websocketConnector = HiveWebsocketConnector.open(wsUri, this, connectionEventHandler);
    }

    public synchronized void close() {
        try {
            websocketConnector.close();
        } catch (Exception ex) {
            logger.error("Error closing Websocket client", ex);
        }
        super.close();
    }

    public synchronized HiveWebsocketConnector getWebsocketConnector() {
        return websocketConnector;
    }

    @Override
    public ApiInfo getInfo() throws HiveException {
        JsonObject request = new JsonObject();
        request.addProperty("action", "server/info");
        String requestId = UUID.randomUUID().toString();
        request.addProperty("requestId", requestId);
        ApiInfo apiInfo = getWebsocketConnector().sendMessage(request, "info", ApiInfo.class, null);
        String restUrl = apiInfo.getRestServerUrl();
        apiInfo = super.getInfo();
        apiInfo.setRestServerUrl(restUrl);
        return apiInfo;
    }

    @Override
    public synchronized String addCommandsSubscription(SubscriptionFilter newFilter,
                                                       HiveMessageHandler<DeviceCommand> handler) throws HiveException {
        Gson gson = GsonFactory.createGson();
        JsonObject request = new JsonObject();
        request.addProperty("action", "command/subscribe");
        request.add("filter", gson.toJsonTree(newFilter));
        return websocketConnector.sendMessage(request, "subscriptionId", String.class, null);
    }

    @Override
    public synchronized String addNotificationsSubscription(SubscriptionFilter newFilter,
                                                            HiveMessageHandler<DeviceNotification> handler)
            throws HiveException {
        Gson gson = GsonFactory.createGson();
        JsonObject request = new JsonObject();
        request.addProperty("action", "notification/subscribe");
        request.add("filter", gson.toJsonTree(newFilter));
        return websocketConnector.sendMessage(request, "subscriptionId", String.class, null);
    }

    @Override
    public synchronized void removeCommandsSubscription(String subId) throws HiveException {
        JsonObject request = new JsonObject();
        request.addProperty("action", "command/unsubscribe");
        request.addProperty("subscriptionId", subId);
        websocketConnector.sendMessage(request);
    }

    public synchronized void removeCommandsSubscription() throws HiveException {
        JsonObject request = new JsonObject();
        request.addProperty("action", "command/unsubscribe");
        websocketConnector.sendMessage(request);
    }

    @Override
    public synchronized void removeNotificationsSubscription(String subId) throws HiveException {
        JsonObject request = new JsonObject();
        request.addProperty("action", "notification/unsubscribe");
        websocketConnector.sendMessage(request);
    }

    @Override
    public synchronized void authenticate(HivePrincipal principal) throws HiveException {
        super.authenticate(principal);
        JsonObject request = new JsonObject();
        request.addProperty("action", "authenticate");
        if (principal.getUser() != null) {
            request.addProperty("login", principal.getUser().getKey());
            request.addProperty("password", principal.getUser().getValue());
        } else if (principal.getDevice() != null) {
            request.addProperty("deviceId", principal.getDevice().getKey());
            request.addProperty("deviceKey", principal.getDevice().getValue());
        } else if (principal.getAccessKey() != null) {
            request.addProperty("accessKey", principal.getAccessKey());
        } else {
            throw new IllegalArgumentException(Messages.INVALID_HIVE_PRINCIPAL);
        }
        websocketConnector.sendMessage(request);
    }
}
