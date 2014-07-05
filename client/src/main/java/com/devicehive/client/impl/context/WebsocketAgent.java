package com.devicehive.client.impl.context;

import com.devicehive.client.HiveMessageHandler;
import com.devicehive.client.impl.json.GsonFactory;
import com.devicehive.client.impl.rest.HiveRestConnector;
import com.devicehive.client.impl.util.Messages;
import com.devicehive.client.impl.websocket.HiveWebsocketConnector;
import com.devicehive.client.model.ApiInfo;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.SubscriptionFilter;
import com.devicehive.client.model.exceptions.HiveException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.DeploymentException;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.*;
import static com.devicehive.client.impl.websocket.JsonEncoder.*;


public class WebsocketAgent extends RestAgent {

    private static Logger logger = LoggerFactory.getLogger(HiveRestConnector.class);
    private final String role;
    private final ConcurrentMap<Long, HiveMessageHandler<DeviceCommand>> commandUpdatesHandlerStorage =
            new ConcurrentHashMap<>();
    private HiveWebsocketConnector websocketConnector;

    public WebsocketAgent(URI restUri, String role) {
        super(restUri);
        this.role = role;
    }

    public synchronized HiveWebsocketConnector getWebsocketConnector() {
        return websocketConnector;
    }

    @Override
    protected synchronized void doConnect() throws HiveException {
        super.doConnect();
        String basicUrl = super.getInfo().getWebSocketServerUrl();
        if (basicUrl == null) {
            throw new HiveException("Can not connect to websockets, endpoint URL is not provided by server");
        }
        URI wsUri = URI.create(basicUrl + "/" + role);
        try {
            this.websocketConnector = new HiveWebsocketConnector(wsUri, this);
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
        super.afterConnect();
        HivePrincipal principal = getHivePrincipal();
        if (principal != null) {
            authenticate(principal);
        }
        Set<String> commandSubIds = new HashSet<>(this.getCommandSubscriptionsStorage().keySet());
        for (String subId : commandSubIds) {
            SubscriptionDescriptor<DeviceCommand> sub = getCommandsSubscriptionDescriptor(subId);
            subscribeForCommands(sub.getFilter(), sub.getHandler(), subId);
        }

        Set<String> notificationSubIds = new HashSet<>(this.getNotificationSubscriptionsStorage().keySet());
        for (String subId : notificationSubIds) {
            SubscriptionDescriptor<DeviceNotification> sub = getNotificationsSubscriptionDescriptor(subId);
            subscribeForNotifications(sub.getFilter(), sub.getHandler(), subId);
        }
    }


    @Override
    public synchronized void close() throws HiveException {
        super.close();
    }

    @Override
    public ApiInfo getInfo() throws HiveException {
        JsonObject request = new JsonObject();
        request.addProperty(ACTION_MEMBER, "server/info");
        String requestId = UUID.randomUUID().toString();
        request.addProperty(REQUEST_ID_MEMBER, requestId);
        ApiInfo apiInfo = getWebsocketConnector().sendMessage(request, "info", ApiInfo.class, null);
        String restUrl = apiInfo.getRestServerUrl();
        apiInfo = super.getInfo();
        apiInfo.setRestServerUrl(restUrl);
        return apiInfo;
    }

    @Override
    public synchronized String subscribeForCommands(SubscriptionFilter newFilter,
                                                    HiveMessageHandler<DeviceCommand> handler) throws HiveException {
        return subscribeForCommands(newFilter, handler, null);
    }


    public synchronized String subscribeForCommands(SubscriptionFilter newFilter,
                                                    HiveMessageHandler<DeviceCommand> handler, String oldId) throws HiveException {
        Gson gson = GsonFactory.createGson();
        JsonObject request = new JsonObject();
        request.addProperty(ACTION_MEMBER, "command/subscribe");
        request.add("filter", gson.toJsonTree(newFilter));
        String subscriptionIdValue = websocketConnector.sendMessage(request, SUBSCRIPTION_ID, String.class, null);
        if (oldId == null) {
            addCommandsSubscription(subscriptionIdValue, new SubscriptionDescriptor<>(handler, newFilter));
        } else {
            replaceCommandSubscription(oldId, subscriptionIdValue, new SubscriptionDescriptor<>(handler, newFilter));
        }
        return subscriptionIdValue;
    }

    @Override
    public synchronized String subscribeForNotifications(SubscriptionFilter newFilter,
                                                         HiveMessageHandler<DeviceNotification> handler) throws HiveException {
        return subscribeForNotifications(newFilter, handler, null);
    }

    public synchronized String subscribeForNotifications(SubscriptionFilter newFilter,
                                                         HiveMessageHandler<DeviceNotification> handler, String oldId)
            throws HiveException {
        Gson gson = GsonFactory.createGson();
        JsonObject request = new JsonObject();
        request.addProperty(ACTION_MEMBER, "notification/subscribe");
        request.add("filter", gson.toJsonTree(newFilter));
        String subscriptionIdValue = websocketConnector.sendMessage(request, SUBSCRIPTION_ID, String.class, null);
        if (oldId == null) {
            addNotificationsSubscription(subscriptionIdValue, new SubscriptionDescriptor<>(handler, newFilter));
        } else {
            replaceNotificationSubscription(oldId, subscriptionIdValue, new SubscriptionDescriptor<>(handler, newFilter));
        }
        return subscriptionIdValue;
    }

    @Override
    public synchronized void unsubscribeFromCommands(String subId) throws HiveException {
        JsonObject request = new JsonObject();
        request.addProperty(ACTION_MEMBER, "command/unsubscribe");
        request.addProperty(SUBSCRIPTION_ID, subId);
        websocketConnector.sendMessage(request);
    }


    public synchronized void unsubscribeFromNotifications(String subId) throws HiveException {
        JsonObject request = new JsonObject();
        request.addProperty(ACTION_MEMBER, "notification/unsubscribe");
        request.addProperty(SUBSCRIPTION_ID, subId);
        websocketConnector.sendMessage(request);
    }

    @Override
    public synchronized void authenticate(HivePrincipal principal) throws HiveException {
        super.authenticate(principal);
        JsonObject request = new JsonObject();
        request.addProperty(ACTION_MEMBER, "authenticate");
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

    public synchronized void addCommandUpdateSubscription(Long commandId, String guid, HiveMessageHandler<DeviceCommand> handler) {
        commandUpdatesHandlerStorage.put(commandId, handler);
    }

    public void handleCommandInsert(JsonObject jsonMessage) throws InterruptedException {
        Gson commandInsertGson = GsonFactory.createGson(COMMAND_LISTED);
        DeviceCommand commandInsert = commandInsertGson.fromJson(jsonMessage.getAsJsonObject(COMMAND_MEMBER),
                DeviceCommand.class);
        SubscriptionDescriptor<DeviceCommand> sub = getCommandsSubscriptionDescriptor(jsonMessage.get(SUBSCRIPTION_ID).getAsString());
        sub.handleMessage(commandInsert);
        logger.debug("Device command inserted. Id: " + commandInsert.getId());

    }

    public void handleCommandUpdate(JsonObject jsonMessage) throws InterruptedException {
        Gson commandUpdateGson = GsonFactory.createGson(COMMAND_UPDATE_TO_CLIENT);
        DeviceCommand commandUpdated = commandUpdateGson.fromJson(jsonMessage.getAsJsonObject
                (COMMAND_MEMBER), DeviceCommand.class);
        if (commandUpdatesHandlerStorage.get(commandUpdated.getId()) != null) {
            commandUpdatesHandlerStorage.remove(commandUpdated.getId()).handle(commandUpdated);
        }
        logger.debug("Device command updated. Id: " + commandUpdated.getId() + ". Status: " +
                commandUpdated.getStatus());
    }

    public void handleNotification(JsonObject jsonMessage) throws InterruptedException {
        Gson notificationsGson = GsonFactory.createGson(NOTIFICATION_TO_CLIENT);
        DeviceNotification notification = notificationsGson.fromJson(jsonMessage.getAsJsonObject
                (NOTIFICATION_MEMBER), DeviceNotification.class);
        SubscriptionDescriptor<DeviceNotification> sub = getNotificationsSubscriptionDescriptor(jsonMessage.get(SUBSCRIPTION_ID).getAsString());
        sub.handleMessage(notification);
        logger.debug("Device notification inserted. Id: " + notification.getId());
    }
}
