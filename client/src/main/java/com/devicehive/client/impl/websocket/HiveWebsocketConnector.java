package com.devicehive.client.impl.websocket;


import com.devicehive.client.impl.context.Constants;
import com.devicehive.client.impl.context.WebsocketAgent;
import com.devicehive.client.impl.json.GsonFactory;
import com.devicehive.client.impl.json.strategies.JsonPolicyDef;
import com.devicehive.client.impl.util.Messages;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.devicehive.client.model.exceptions.HiveException;
import com.devicehive.client.model.exceptions.HiveServerException;
import com.devicehive.client.model.exceptions.InternalHiveClientException;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import org.glassfish.tyrus.client.ClientManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static javax.ws.rs.core.Response.Status.Family;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

/**
 * Part of client that creates requests based on required parameters (set by user) and parses responses into model
 * classes representation
 */
public class HiveWebsocketConnector {

    private static final String REQUEST_ID_MEMBER = "requestId";
    private static final Long WAIT_TIMEOUT = 1L;
    private static Logger logger = LoggerFactory.getLogger(HiveWebsocketConnector.class);
    private final ConcurrentMap<String, SettableFuture<JsonObject>> websocketResponsesMap = new ConcurrentHashMap<>();
    private final ReadWriteLock currentSessionLock = new ReentrantReadWriteLock(true);
    private Session currentSession;


    /**
     * Creates client connected to the given websocket URL. All state is kept in the hive context.
     *
     * @param uri URI of websocket service
     */
    public HiveWebsocketConnector(URI uri, final WebsocketAgent websocketAgent) throws
            IOException,
            DeploymentException {
        getClient().connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig config) {
                try {
                    currentSessionLock.writeLock().lock();
                    logger.info("[onOpen] User session: {}", session);
                    SessionMonitor sessionMonitor = new SessionMonitor(session);
                    session.getUserProperties().put(SessionMonitor.SESSION_MONITOR_KEY, sessionMonitor);
                    session.addMessageHandler(new HiveWebsocketHandler(websocketAgent, websocketResponsesMap));
                    currentSession = session;
                } finally {
                    currentSessionLock.writeLock().unlock();
                }
            }

            @Override
            public void onClose(Session session, CloseReason reason) {
                try {
                    currentSessionLock.writeLock().lock();
                    logger.info("[onClose] Websocket client closed. Reason: " + reason.getReasonPhrase() + "; Code: " +
                            reason.getCloseCode
                                    ().getCode());
                    SessionMonitor sessionMonitor =
                            (SessionMonitor) session.getUserProperties().get(SessionMonitor.SESSION_MONITOR_KEY);
                    if (sessionMonitor != null) {
                        sessionMonitor.close();
                    }
                } finally {
                    currentSessionLock.writeLock().unlock();
                }
            }

            @Override
            public void onError(Session session, Throwable thr) {
                logger.error("[onError] ", thr);
            }
        }, ClientEndpointConfig.Builder.create().build(), uri);

    }


    private static ClientManager getClient() {
        ClientManager client = ClientManager.createClient();
        ClientManager.ReconnectHandler reconnectHandler = new ClientManager.ReconnectHandler() {
            @Override
            public boolean onDisconnect(CloseReason closeReason) {
                return CloseReason.CloseCodes.NORMAL_CLOSURE != closeReason.getCloseCode();
            }

            @Override
            public boolean onConnectFailure(Exception exception) {
                return super.onConnectFailure(exception);
            }
        };
        client.getProperties().put(ClientManager.RECONNECT_HANDLER, reconnectHandler);
        return client;
    }

    /**
     * Sends message to server
     *
     * @param message some HiveEntity object in JSON
     */
    public void sendMessage(JsonObject message) throws HiveException {
        String requestId = rawSend(message);
        websocketResponsesMap.put(requestId, SettableFuture.<JsonObject>create());
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
    public <T> T sendMessage(JsonObject message, String responseMemberName, Type typeOfResponse,
                             JsonPolicyDef.Policy policy) throws HiveException {
        String requestId = rawSend(message);
        websocketResponsesMap.put(requestId, SettableFuture.<JsonObject>create());
        return processResponse(requestId, responseMemberName, typeOfResponse, policy);
    }

    private String rawSend(JsonObject message) {
        String requestId = UUID.randomUUID().toString();
        message.addProperty(REQUEST_ID_MEMBER, requestId);
        try {
            currentSessionLock.readLock().lock();
            currentSession.getAsyncRemote().sendObject(message);
            return requestId;
        } finally {
            currentSessionLock.readLock().unlock();
        }
    }

    /**
     * Implementation of close method in closeable interface. Closes endpoint.
     *
     * @throws IOException
     */

    public void close() {
        try {
            currentSessionLock.writeLock().lock();
            currentSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, ""));
        } catch (IOException e) {
            logger.error("Error closing websocket session", e);
        } finally {
            currentSessionLock.writeLock().unlock();
        }
    }

    //Private methods-----------------------------------------------------------------------------------------------
    private void processResponse(final String requestId) throws HiveException {
        try {
            JsonObject result = websocketResponsesMap.get(requestId).get(WAIT_TIMEOUT, TimeUnit.MINUTES);

            if (result != null) {
                Gson gson = GsonFactory.createGson();
                SimpleWebsocketResponse response;
                try {
                    response = gson.fromJson(result, SimpleWebsocketResponse.class);
                } catch (JsonSyntaxException e) {
                    throw new HiveServerException(Messages.INCORRECT_RESPONSE_TYPE, 500);
                }
                if (response.getStatus().equals(Constants.EXPECTED_RESPONSE_STATUS)) {
                    logger.debug("Request with id:" + requestId + "proceed successfully");
                    return;
                }
                Family errorFamily = Family.familyOf(response.getCode());
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
            logger.warn("For request id: " + requestId + ". Error message: " + e.getMessage(), e);
        } catch (TimeoutException e) {
            noResponseAction();
        } catch (ExecutionException e) {
            logger.warn("For request id: " + requestId + ". Error message: " + e.getMessage(), e);
            throw new InternalHiveClientException(e.getMessage(), e);
        } finally {
            websocketResponsesMap.remove(requestId);
        }
    }

    private <T> T processResponse(final String requestId, final String responseMemberName, final Type typeOfResponse,
                                  final JsonPolicyDef.Policy receivePolicy) throws HiveException {
        try {
            JsonObject result = websocketResponsesMap.get(requestId).get(WAIT_TIMEOUT, TimeUnit.MINUTES);
            if (result != null) {
                if (result.get(Constants.STATUS).getAsString().equals(Constants.EXPECTED_RESPONSE_STATUS)) {
                    logger.debug("Request with id:" + requestId + "proceed successfully");
                } else {
                    Family errorFamily = Family.familyOf(result.get(Constants.CODE).getAsInt());
                    String error = null;
                    if (result.get(Constants.ERROR) instanceof JsonPrimitive)
                        error = result.get(Constants.ERROR).getAsString();
                    Integer code = null;
                    if (result.get(Constants.CODE) instanceof JsonPrimitive)
                        result.get(Constants.CODE).getAsInt();
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
                Gson gson = GsonFactory.createGson(receivePolicy);
                T response;
                try {
                    response = gson.fromJson(result.get(responseMemberName), typeOfResponse);
                } catch (JsonSyntaxException e) {
                    throw new InternalHiveClientException("Wrong type of response!");
                }
                return response;
            }
        } catch (InterruptedException e) {
            logger.warn("For request id: " + requestId + ". Error message: " + e.getMessage(), e);
        } catch (TimeoutException e) {
            noResponseAction();
        } catch (ExecutionException e) {
            logger.warn("For request id: " + requestId + ". Error message: " + e.getMessage(), e);
            throw new InternalHiveClientException(e.getMessage(), e);
        } catch (Exception e) {
            logger.warn("For request id: " + requestId + ". Error message: " + e.getMessage(), e);
        } finally {
            websocketResponsesMap.remove(requestId);
        }
        return null;
    }

    private void noResponseAction() throws HiveServerException {
        throw new HiveServerException(Messages.NO_RESPONSES_FROM_SERVER, SERVICE_UNAVAILABLE.getStatusCode());
    }
}
