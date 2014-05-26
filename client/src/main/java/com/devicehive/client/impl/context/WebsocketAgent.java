package com.devicehive.client.impl.context;

import com.devicehive.client.HiveMessageHandler;
import com.devicehive.client.impl.context.connection.HiveConnectionEventHandler;
import com.devicehive.client.impl.json.GsonFactory;
import com.devicehive.client.impl.util.Messages;
import com.devicehive.client.model.ApiInfo;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.SubscriptionFilter;
import com.devicehive.client.model.exceptions.HiveException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.DeploymentException;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class WebsocketAgent extends RestAgent {

    private static Logger logger = LoggerFactory.getLogger(HiveRestConnector.class);
    private final String role;
    private final ConcurrentMap<Long, HiveMessageHandler<DeviceCommand>> commandUpdatesHandlerStorage =
            new ConcurrentHashMap<>();
    private HiveWebsocketConnector websocketConnector;

    public WebsocketAgent(URI restUri, String role, HiveConnectionEventHandler connectionEventHandler) {
        super(restUri, connectionEventHandler);
        this.role = role;
    }

    public synchronized HiveWebsocketConnector getWebsocketConnector() {
        return websocketConnector;
    }

    @Override
    protected synchronized void doConnect() throws HiveException {
        super.doConnect();
        URI wsUri = URI.create(super.getInfo().getWebSocketServerUrl() + "/" + role);
        try {
            this.websocketConnector = new HiveWebsocketConnector(wsUri, this, connectionEventHandler);
        } catch (IOException | DeploymentException e) {
            throw new HiveException("Can not connect to websockets", e);
        }
    }

    @Override
    protected synchronized void doDisconnect() {
        websocketConnector.close();
        super.doDisconnect();
    }

    @Override
    protected void afterDisconnect() throws HiveException {
        super.afterDisconnect();
    }

    @Override
    protected void afterConnect() throws HiveException {
        HivePrincipal principal = getHivePrincipal();
        if (principal != null) {
            authenticate(principal);
        }
        super.afterConnect();
    }

    @Override
    synchronized protected void resubscribe() throws HiveException {
        Map<String, SubscriptionDescriptor<DeviceCommand>> commandSubscriptions =
                ObjectUtils.cloneIfPossible(getCommandSubscriptionsStorage());
        getCommandSubscriptionsStorage().clear();
        for (Map.Entry<String, SubscriptionDescriptor<DeviceCommand>> subscription : commandSubscriptions.entrySet()) {
            SubscriptionDescriptor<DeviceCommand> subscriptionValue = subscription.getValue();
            addCommandsSubscription(subscriptionValue.getFilter(),
                    subscriptionValue.getHandler(),
                    subscription.getKey());
        }
        Map<String, SubscriptionDescriptor<DeviceNotification>> notificationSubscriptions =
                ObjectUtils.cloneIfPossible(getNotificationSubscriptionsStorage());
        getNotificationSubscriptionsStorage().clear();
        for (Map.Entry<String, SubscriptionDescriptor<DeviceNotification>> subscription :
                notificationSubscriptions.entrySet()) {
            SubscriptionDescriptor<DeviceNotification> subscriptionValue = subscription.getValue();
            addNotificationsSubscription(subscriptionValue.getFilter(),
                    subscriptionValue.getHandler(),
                    subscription.getKey());
        }
    }

    @Override
    public synchronized void close() throws HiveException {
        super.close();
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

    @Override
    public synchronized void addCommandUpdateSubscription(Long commandId,String guid, HiveMessageHandler<DeviceCommand> handler) {
        commandUpdatesHandlerStorage.put(commandId, handler);
    }

    public synchronized void proccessCommandUpdate(DeviceCommand commandUpdated) {
        if (commandUpdatesHandlerStorage.get(commandUpdated.getId()) != null) {
            commandUpdatesHandlerStorage.get(commandUpdated.getId()).handle(commandUpdated);
            commandUpdatesHandlerStorage.remove(commandUpdated.getId());
        }
    }
}
