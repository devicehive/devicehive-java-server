package com.devicehive.client.impl.context;

import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import com.devicehive.client.ConnectionLostCallback;
import com.devicehive.client.ConnectionRestoredCallback;
import com.devicehive.client.HiveMessageHandler;
import com.devicehive.client.impl.json.GsonFactory;
import com.devicehive.client.impl.json.strategies.JsonPolicyDef;
import com.devicehive.client.impl.util.Messages;
import com.devicehive.client.impl.websocket.HiveWebsocketHandler;
import com.devicehive.client.impl.websocket.SessionMonitor;
import com.devicehive.client.impl.websocket.SimpleWebsocketResponse;
import com.devicehive.client.model.ApiInfo;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.SubscriptionFilter;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.devicehive.client.model.exceptions.HiveException;
import com.devicehive.client.model.exceptions.HiveServerException;
import com.devicehive.client.model.exceptions.InternalHiveClientException;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.ws.rs.core.Response;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.COMMAND_LISTED;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.COMMAND_UPDATE_TO_CLIENT;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT;
import static com.devicehive.client.impl.websocket.JsonEncoder.ACTION_MEMBER;
import static com.devicehive.client.impl.websocket.JsonEncoder.COMMAND_INSERT;
import static com.devicehive.client.impl.websocket.JsonEncoder.COMMAND_MEMBER;
import static com.devicehive.client.impl.websocket.JsonEncoder.COMMAND_UPDATE;
import static com.devicehive.client.impl.websocket.JsonEncoder.NOTIFICATION_INSERT;
import static com.devicehive.client.impl.websocket.JsonEncoder.NOTIFICATION_MEMBER;
import static com.devicehive.client.impl.websocket.JsonEncoder.SUBSCRIPTION_ID;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;


public class WebsocketAgent extends RestAgent {
    private static final Logger logger = LoggerFactory.getLogger(WebsocketAgent.class);

    private static final String REQUEST_ID_MEMBER = "requestId";
    private static final String EXPECTED_RESPONSE_STATUS = "success";
    private static final Long WAIT_TIMEOUT = 1L;
    private static final String STATUS = "status";
    private static final String CODE = "code";
    private static final String ERROR = "error";

    private final String role;

    private final ConcurrentMap<String, String> serverToLocalSubIdMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, HiveMessageHandler<DeviceCommand>> commandUpdatesHandlerStorage =
        new ConcurrentHashMap<>();
    private final ConcurrentMap<String, SettableFuture<JsonObject>> websocketResponsesMap = new ConcurrentHashMap<>();

    private final Endpoint endpoint;
    private Session currentSession;

    private final ExecutorService connectionStateExecutor = Executors.newSingleThreadExecutor();
    private final ConnectionLostCallback connectionLostCallback;
    private final ConnectionRestoredCallback connectionRestoredCallback;

    public WebsocketAgent(final ConnectionLostCallback connectionLostCallback,
                          final ConnectionRestoredCallback connectionRestoredCallback,
                          final URI restUri, final String role) {
        super(restUri);
        this.connectionLostCallback = connectionLostCallback;
        this.connectionRestoredCallback = connectionRestoredCallback;
        this.role = role;
        this.endpoint = new EndpointFactory().createEndpoint();
    }

    private ClientManager createClient() {
        final ClientManager client = ClientManager.createClient();
        final ClientManager.ReconnectHandler reconnectHandler = new ClientManager.ReconnectHandler() {
            @Override
            public boolean onDisconnect(CloseReason closeReason) {
                final boolean lost = CloseReason.CloseCodes.NORMAL_CLOSURE != closeReason.getCloseCode();
                if (lost) {
                    connectionStateExecutor.submit(new Runnable() {
                        @Override
                        public void run() {
                            if (connectionLostCallback != null) {
                                connectionLostCallback.connectionLost();
                            }
                        }
                    });
                }
                return lost;
            }

            @Override
            public boolean onConnectFailure(final Exception exception) {
                return super.onConnectFailure(exception);
            }
        };
        client.getProperties().put(ClientProperties.RECONNECT_HANDLER, reconnectHandler);
        return client;
    }

    @Override
    protected void doConnect() throws HiveException {
        super.doConnect();
        final String basicUrl = super.getInfo().getWebSocketServerUrl();
        if (basicUrl == null) {
            throw new HiveException("Can not connect to websockets, endpoint URL is not provided by server");
        }
        final URI wsUri = URI.create(basicUrl + "/" + role);
        try {
            createClient().connectToServer(endpoint, ClientEndpointConfig.Builder.create().build(), wsUri);
        } catch (IOException | DeploymentException e) {
            throw new HiveException("Cannot connect to websockets", e);
        }
    }

    @Override
    protected void doDisconnect() {
        try {
            if (currentSession != null) {
                currentSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Bye."));
            }
        } catch (IOException e) {
            logger.error("Error closing websocket session", e);
        }
        super.doDisconnect();
    }

    private void resubscribe() throws HiveException {
        final HivePrincipal principal = getHivePrincipal();
        if (principal != null) {
            authenticate(principal);
        }

        subscriptionsLock.readLock().lock();
        try {
            for (final Map.Entry<String, SubscriptionDescriptor<DeviceCommand>> entry : commandSubscriptionsStorage.entrySet()) {
                serverToLocalSubIdMap.put(sendSubscribeForCommands(entry.getValue().getFilter()), entry.getKey());
            }

            for (final Map.Entry<String, SubscriptionDescriptor<DeviceNotification>> entry : notificationSubscriptionsStorage.entrySet()) {
                serverToLocalSubIdMap.put(sendSubscribeForNotifications(entry.getValue().getFilter()), entry.getKey());
            }
        } finally {
            subscriptionsLock.readLock().unlock();
        }
    }

    /**
     * Sends message to server
     *
     * @param message some HiveEntity object in JSON
     */
    public void sendMessage(final JsonObject message) throws HiveException {
        final String requestId = UUID.randomUUID().toString();
        websocketResponsesMap.put(requestId, SettableFuture.<JsonObject>create());
        message.addProperty(REQUEST_ID_MEMBER, requestId);
        rawSend(message);
        processResponse(requestId);
    }

    /**
     * Sends message to server
     *
     * @param message            some HiveEntity object in JSON
     * @param responseMemberName in response name of field that contains required object
     * @param typeOfResponse     type of response
     * @param policy             policy that declares exclusion strategy for received object
     * @return instance of typeOfResponse, that represents server's response
     */
    public <T> T sendMessage(final JsonObject message, final String responseMemberName, final Type typeOfResponse,
                             final JsonPolicyDef.Policy policy) throws HiveException {
        final String requestId = UUID.randomUUID().toString();
        websocketResponsesMap.put(requestId, SettableFuture.<JsonObject>create());
        message.addProperty(REQUEST_ID_MEMBER, requestId);
        rawSend(message);
        return processResponse(requestId, responseMemberName, typeOfResponse, policy);
    }

    private void rawSend(final JsonObject message) {
        connectionLock.readLock().lock();
        try {
            currentSession.getAsyncRemote().sendObject(message);
        } finally {
            connectionLock.readLock().unlock();
        }
    }

    private void processResponse(final String requestId) throws HiveException {
        try {
            final JsonObject result = websocketResponsesMap.get(requestId).get(WAIT_TIMEOUT, TimeUnit.MINUTES);

            if (result != null) {
                final Gson gson = GsonFactory.createGson();
                final SimpleWebsocketResponse response;
                try {
                    response = gson.fromJson(result, SimpleWebsocketResponse.class);
                } catch (JsonSyntaxException e) {
                    throw new HiveServerException(Messages.INCORRECT_RESPONSE_TYPE, 500);
                }
                if (response.getStatus().equals(EXPECTED_RESPONSE_STATUS)) {
                    logger.debug("Request with id:" + requestId + "proceed successfully");
                    return;
                }
                final Response.Status.Family errorFamily = Response.Status.Family.familyOf(response.getCode());
                switch (errorFamily) {
                    case SERVER_ERROR:
                        logger.warn(
                            "Request id: " + requestId + ". Error message:" + response.getError() + ". Status code:"
                            + response.getCode()
                        );
                        throw new HiveServerException(response.getError(), response.getCode());
                    case CLIENT_ERROR:
                        logger.warn(
                            "Request id: " + requestId + ". Error message:" + response.getError() + ". Status code:"
                            + response.getCode()
                        );
                        throw new HiveClientException(response.getError(), response.getCode());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HiveClientException("Interrupted", e);
        } catch (TimeoutException e) {
            throw new HiveServerException(Messages.NO_RESPONSES_FROM_SERVER, SERVICE_UNAVAILABLE.getStatusCode());
        } catch (ExecutionException e) {
            throw new InternalHiveClientException(e.getMessage(), e.getCause());
        } finally {
            websocketResponsesMap.remove(requestId);
        }
    }

    private <T> T processResponse(final String requestId, final String responseMemberName, final Type typeOfResponse,
                                  final JsonPolicyDef.Policy receivePolicy) throws HiveException {
        try {
            final JsonObject result = websocketResponsesMap.get(requestId).get(WAIT_TIMEOUT, TimeUnit.MINUTES);
            if (result != null) {
                if (result.get(STATUS).getAsString().equals(EXPECTED_RESPONSE_STATUS)) {
                    logger.debug("Request with id:" + requestId + "proceed successfully");
                } else {
                    final Response.Status.Family
                        errorFamily =
                        Response.Status.Family.familyOf(result.get(CODE).getAsInt());
                    String error = null;
                    if (result.get(ERROR) instanceof JsonPrimitive) {
                        error = result.get(ERROR).getAsString();
                    }
                    Integer code = null;
                    if (result.get(CODE) instanceof JsonPrimitive) {
                        code = result.get(CODE).getAsInt();
                    }
                    switch (errorFamily) {
                        case SERVER_ERROR:
                            logger.warn("Request id: " + requestId + ". Error message:" + error + ". Status " +
                                        "code:" + code);
                            throw new HiveServerException(error, code);
                        case CLIENT_ERROR:
                            logger.warn("Request id: " + requestId + ". Error message:" + error + ". Status " +
                                        "code:" + code);
                            throw new HiveClientException(error, code);
                    }
                }
                final Gson gson = GsonFactory.createGson(receivePolicy);
                final T response;
                try {
                    response = gson.fromJson(result.get(responseMemberName), typeOfResponse);
                } catch (JsonSyntaxException e) {
                    throw new InternalHiveClientException(Messages.WRONG_TYPE_RESPONSE);
                }
                return response;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HiveClientException(e.getMessage(), e);
        } catch (TimeoutException e) {
            noResponseAction();
        } catch (ExecutionException e) {
            throw new InternalHiveClientException(e.getMessage(), e.getCause());
        } finally {
            websocketResponsesMap.remove(requestId);
        }
        return null;
    }

    private void noResponseAction() throws HiveServerException {
        throw new HiveServerException(Messages.NO_RESPONSES_FROM_SERVER, SERVICE_UNAVAILABLE.getStatusCode());
    }

    @Override
    public ApiInfo getInfo() throws HiveException {
        final JsonObject request = new JsonObject();
        request.addProperty(ACTION_MEMBER, "server/info");
        final String requestId = UUID.randomUUID().toString();
        request.addProperty(REQUEST_ID_MEMBER, requestId);
        ApiInfo apiInfo = sendMessage(request, "info", ApiInfo.class, null);
        final String restUrl = apiInfo.getRestServerUrl();
        apiInfo = super.getInfo();
        apiInfo.setRestServerUrl(restUrl);
        return apiInfo;
    }

    @Override
    public String subscribeForCommands(final SubscriptionFilter newFilter,
                                       final HiveMessageHandler<DeviceCommand> handler) throws HiveException {
        subscriptionsLock.writeLock().lock();
        try {
            final String localId = UUID.randomUUID().toString();
            serverToLocalSubIdMap.put(sendSubscribeForCommands(newFilter), localId);
            commandSubscriptionsStorage.put(localId, new SubscriptionDescriptor<>(handler, newFilter));
            return localId;
        } finally {
            subscriptionsLock.writeLock().unlock();
        }
    }

    private String sendSubscribeForCommands(final SubscriptionFilter newFilter) throws HiveException {
        final Gson gson = GsonFactory.createGson();
        final JsonObject request = new JsonObject();
        request.addProperty(ACTION_MEMBER, "command/subscribe");
        request.add("filter", gson.toJsonTree(newFilter));
        return sendMessage(request, SUBSCRIPTION_ID, String.class, null);
    }

    @Override
    public String subscribeForNotifications(final SubscriptionFilter newFilter,
                                            final HiveMessageHandler<DeviceNotification> handler) throws HiveException {
        subscriptionsLock.writeLock().lock();
        try {
            final String localId = UUID.randomUUID().toString();
            serverToLocalSubIdMap.put(sendSubscribeForNotifications(newFilter), localId);
            notificationSubscriptionsStorage.put(localId, new SubscriptionDescriptor<>(handler, newFilter));
            return localId;
        } finally {
            subscriptionsLock.writeLock().unlock();
        }
    }

    private String sendSubscribeForNotifications(final SubscriptionFilter newFilter) throws HiveException {
        final Gson gson = GsonFactory.createGson();
        final JsonObject request = new JsonObject();
        request.addProperty(ACTION_MEMBER, "notification/subscribe");
        request.add("filter", gson.toJsonTree(newFilter));
        return sendMessage(request, SUBSCRIPTION_ID, String.class, null);
    }

    @Override
    public void unsubscribeFromCommands(final String subId) throws HiveException {
        subscriptionsLock.writeLock().lock();
        try {
            commandSubscriptionsStorage.remove(subId);
            final JsonObject request = new JsonObject();
            request.addProperty(ACTION_MEMBER, "command/unsubscribe");
            request.addProperty(SUBSCRIPTION_ID, subId);
            sendMessage(request);
        } finally {
            subscriptionsLock.writeLock().unlock();
        }
    }

    public void unsubscribeFromNotifications(final String subId) throws HiveException {
        subscriptionsLock.writeLock().lock();
        try {
            notificationSubscriptionsStorage.remove(subId);
            final JsonObject request = new JsonObject();
            request.addProperty(ACTION_MEMBER, "notification/unsubscribe");
            request.addProperty(SUBSCRIPTION_ID, subId);
            sendMessage(request);
        } finally {
            subscriptionsLock.writeLock().unlock();
        }
    }

    @Override
    public void authenticate(final HivePrincipal principal) throws HiveException {
        super.authenticate(principal);
        final JsonObject request = new JsonObject();
        request.addProperty(ACTION_MEMBER, "authenticate");
        if (principal.isUser()) {
            request.addProperty("login", principal.getPrincipal().getLeft());
            request.addProperty("password", principal.getPrincipal().getRight());
        } else if (principal.isDevice()) {
            request.addProperty("deviceId", principal.getPrincipal().getLeft());
            request.addProperty("deviceKey", principal.getPrincipal().getRight());
        } else if (principal.isAccessKey()) {
            request.addProperty("accessKey", principal.getPrincipal().getValue());
        } else {
            throw new IllegalArgumentException(Messages.INVALID_HIVE_PRINCIPAL);
        }
        sendMessage(request);
    }

    public void addCommandUpdateSubscription(final Long commandId, final HiveMessageHandler<DeviceCommand> handler) {
        subscriptionsLock.writeLock().lock();
        try {
            commandUpdatesHandlerStorage.put(commandId, handler);
        } finally {
            subscriptionsLock.writeLock().unlock();
        }
    }

    public void handleServerMessage(final JsonObject jsonMessage) {
        subscriptionExecutor.submit(new Runnable() {
            @Override
            public void run() {
                subscriptionsLock.readLock().lock();
                try {
                    switch (jsonMessage.get(ACTION_MEMBER).getAsString()) {
                        case COMMAND_INSERT:
                            final Gson commandInsertGson = GsonFactory.createGson(COMMAND_LISTED);
                            final DeviceCommand
                                commandInsert =
                                commandInsertGson.fromJson(jsonMessage.getAsJsonObject(COMMAND_MEMBER),
                                                           DeviceCommand.class);
                            final String
                                localCommandSubId =
                                serverToLocalSubIdMap.get(jsonMessage.get(SUBSCRIPTION_ID).getAsString());
                            commandSubscriptionsStorage.get(localCommandSubId).handleMessage(commandInsert);
                            break;
                        case COMMAND_UPDATE:
                            final Gson commandUpdateGson = GsonFactory.createGson(COMMAND_UPDATE_TO_CLIENT);
                            final DeviceCommand commandUpdated = commandUpdateGson.fromJson(jsonMessage.getAsJsonObject
                                (COMMAND_MEMBER), DeviceCommand.class);
                            if (commandUpdatesHandlerStorage.get(commandUpdated.getId()) != null) {
                                commandUpdatesHandlerStorage.remove(commandUpdated.getId()).handle(commandUpdated);
                            }
                            break;
                        case NOTIFICATION_INSERT:
                            final Gson notificationsGson = GsonFactory.createGson(NOTIFICATION_TO_CLIENT);
                            final DeviceNotification notification = notificationsGson.fromJson(jsonMessage.getAsJsonObject
                                (NOTIFICATION_MEMBER), DeviceNotification.class);
                            final String
                                localNotifSubId =
                                serverToLocalSubIdMap.get(jsonMessage.get(SUBSCRIPTION_ID).getAsString());
                            notificationSubscriptionsStorage.get(localNotifSubId).handleMessage(notification);
                            break;
                        default: //unknown request
                            logger.error("Server sent unknown message {}", jsonMessage);
                    }
                } catch (InternalHiveClientException e) {
                    logger.error("Cannot retrieve gson from a factory {}", e.getMessage());
                } finally {
                    subscriptionsLock.readLock().unlock();
                }
            }
        });
    }

    private final class EndpointFactory {

        private EndpointFactory() {
        }

        private Endpoint createEndpoint() {
            return new Endpoint() {
                @Override
                public void onOpen(final Session session, final EndpointConfig config) {
                    logger.info("[onOpen] User session: {}", session);
                    final SessionMonitor sessionMonitor = new SessionMonitor(session);
                    session.getUserProperties().put(SessionMonitor.SESSION_MONITOR_KEY, sessionMonitor);
                    session.addMessageHandler(new HiveWebsocketHandler(WebsocketAgent.this, websocketResponsesMap));
                    final boolean reconnect = currentSession != null;
                    currentSession = session;
                    if (reconnect) {
                        try {
                            resubscribe();
                            connectionStateExecutor.submit(new Runnable() {
                                @Override
                                public void run() {
                                    if (connectionRestoredCallback != null) {
                                        connectionRestoredCallback.connectionRestored();
                                    }
                                }
                            });
                        } catch (final HiveException he) {
                            logger.error("Can not restore session context", he);
                            connectionStateExecutor.submit(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION,
                                                                      "Can not restore session context"));
                                    } catch (IOException e1) {
                                        logger.error("Can not close session", e1);
                                    }
                                }
                            });
                        }
                    }
                }

                @Override
                public void onClose(final Session session, final CloseReason reason) {
                    logger.info(
                        "[onClose] Websocket client closed. Reason: " + reason.getReasonPhrase() + "; Code: " +
                        reason.getCloseCode
                            ().getCode());
                    final SessionMonitor sessionMonitor =
                        (SessionMonitor) session.getUserProperties().get(SessionMonitor.SESSION_MONITOR_KEY);
                    if (sessionMonitor != null) {
                        sessionMonitor.close();
                    }
                }

                @Override
                public void onError(Session session, Throwable thr) {
                    logger.error("[onError] ", thr);
                }
            };
        }
    }

}
