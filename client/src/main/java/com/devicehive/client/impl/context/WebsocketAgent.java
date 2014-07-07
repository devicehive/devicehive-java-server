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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.*;
import static com.devicehive.client.impl.websocket.JsonEncoder.*;


public class WebsocketAgent extends RestAgent {

    private static Logger logger = LoggerFactory.getLogger(HiveRestConnector.class);
    private final String role;

    private final ConcurrentMap<String, String> serverToLocalSubIdMap = new ConcurrentHashMap<>();

    private final ConcurrentMap<Long, HiveMessageHandler<DeviceCommand>> commandUpdatesHandlerStorage =
            new ConcurrentHashMap<>();

    private final ExecutorService subscriptionExecutor = Executors.newFixedThreadPool(50);

    private HiveWebsocketConnector websocketConnector;


    public WebsocketAgent(URI restUri, String role) {
        super(restUri);
        this.role = role;
    }

    public HiveWebsocketConnector getWebsocketConnector() {
        stateLock.readLock().lock();
        try {
            return websocketConnector;
        } finally {
            stateLock.readLock().unlock();
        }
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
    protected void doDisconnect() {
        websocketConnector.close();
        super.doDisconnect();
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
            serverToLocalSubIdMap.put(sendSubscribeForCommands(sub.getFilter()), subId);
        }

        Set<String> notificationSubIds = new HashSet<>(this.getNotificationSubscriptionsStorage().keySet());
        for (String subId : notificationSubIds) {
            SubscriptionDescriptor<DeviceNotification> sub = getNotificationsSubscriptionDescriptor(subId);
            serverToLocalSubIdMap.put(sendSubscribeForNotifications(sub.getFilter()), subId);
        }
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
    public String subscribeForCommands(SubscriptionFilter newFilter,
                                       HiveMessageHandler<DeviceCommand> handler) throws HiveException {
        stateLock.writeLock().lock();
        try {
            String localId = UUID.randomUUID().toString();
            addCommandsSubscription(localId, new SubscriptionDescriptor<>(handler, newFilter));
            serverToLocalSubIdMap.put(sendSubscribeForCommands(newFilter), localId);
            return localId;
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    private String sendSubscribeForCommands(SubscriptionFilter newFilter) throws HiveException {
        Gson gson = GsonFactory.createGson();
        JsonObject request = new JsonObject();
        request.addProperty(ACTION_MEMBER, "command/subscribe");
        request.add("filter", gson.toJsonTree(newFilter));
        return websocketConnector.sendMessage(request, SUBSCRIPTION_ID, String.class, null);
    }


    @Override
    public String subscribeForNotifications(SubscriptionFilter newFilter,
                                            HiveMessageHandler<DeviceNotification> handler) throws HiveException {
        stateLock.writeLock().lock();
        try {
            String localId = UUID.randomUUID().toString();
            addNotificationsSubscription(localId, new SubscriptionDescriptor<>(handler, newFilter));
            serverToLocalSubIdMap.put(sendSubscribeForNotifications(newFilter), localId);
            return localId;
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    private String sendSubscribeForNotifications(SubscriptionFilter newFilter) throws HiveException {
        Gson gson = GsonFactory.createGson();
        JsonObject request = new JsonObject();
        request.addProperty(ACTION_MEMBER, "notification/subscribe");
        request.add("filter", gson.toJsonTree(newFilter));
        return websocketConnector.sendMessage(request, SUBSCRIPTION_ID, String.class, null);
    }


    @Override
    public void unsubscribeFromCommands(String subId) throws HiveException {
        stateLock.writeLock().lock();
        try {
            removeCommandsSubscription(subId);
            JsonObject request = new JsonObject();
            request.addProperty(ACTION_MEMBER, "command/unsubscribe");
            request.addProperty(SUBSCRIPTION_ID, subId);
            websocketConnector.sendMessage(request);
        } finally {
            stateLock.writeLock().unlock();
        }
    }


    public void unsubscribeFromNotifications(String subId) throws HiveException {
        stateLock.writeLock().lock();
        try {
            removeNotificationsSubscription(subId);
            JsonObject request = new JsonObject();
            request.addProperty(ACTION_MEMBER, "notification/unsubscribe");
            request.addProperty(SUBSCRIPTION_ID, subId);
            websocketConnector.sendMessage(request);
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    @Override
    public void authenticate(HivePrincipal principal) throws HiveException {
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

        stateLock.writeLock().lock();
        try {
            super.authenticate(principal);
            websocketConnector.sendMessage(request);
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    public void addCommandUpdateSubscription(Long commandId, String guid, HiveMessageHandler<DeviceCommand> handler) {
        stateLock.writeLock().lock();
        try {
            commandUpdatesHandlerStorage.put(commandId, handler);
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    public void handleServerMessage(final JsonObject jsonMessage) {
        subscriptionExecutor.submit(new Runnable() {
            @Override
            public void run() {
                stateLock.readLock().lock();
                try {
                    switch (jsonMessage.get(ACTION_MEMBER).getAsString()) {
                        case COMMAND_INSERT:
                            Gson commandInsertGson = GsonFactory.createGson(COMMAND_LISTED);
                            DeviceCommand commandInsert = commandInsertGson.fromJson(jsonMessage.getAsJsonObject(COMMAND_MEMBER),
                                    DeviceCommand.class);
                            String localCommandSubId = serverToLocalSubIdMap.get(jsonMessage.get(SUBSCRIPTION_ID).getAsString());
                            getCommandsSubscriptionDescriptor(localCommandSubId).handleMessage(commandInsert);
                            break;
                        case COMMAND_UPDATE:
                            Gson commandUpdateGson = GsonFactory.createGson(COMMAND_UPDATE_TO_CLIENT);
                            DeviceCommand commandUpdated = commandUpdateGson.fromJson(jsonMessage.getAsJsonObject
                                    (COMMAND_MEMBER), DeviceCommand.class);
                            if (commandUpdatesHandlerStorage.get(commandUpdated.getId()) != null) {
                                commandUpdatesHandlerStorage.remove(commandUpdated.getId()).handle(commandUpdated);
                            }
                            break;
                        case NOTIFICATION_INSERT:
                            Gson notificationsGson = GsonFactory.createGson(NOTIFICATION_TO_CLIENT);
                            DeviceNotification notification = notificationsGson.fromJson(jsonMessage.getAsJsonObject
                                    (NOTIFICATION_MEMBER), DeviceNotification.class);
                            String localNotifSubId = serverToLocalSubIdMap.get(jsonMessage.get(SUBSCRIPTION_ID).getAsString());
                            getNotificationsSubscriptionDescriptor(localNotifSubId).handleMessage(notification);
                            break;
                        default: //unknown request
                            logger.error("Server sent unknown message {}", jsonMessage);
                    }
                } finally {
                    stateLock.readLock().unlock();
                }
            }
        });
    }
}
