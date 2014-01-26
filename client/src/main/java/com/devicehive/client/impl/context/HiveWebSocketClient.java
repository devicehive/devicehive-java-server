package com.devicehive.client.impl.context;


import com.devicehive.client.HiveClient;
import com.devicehive.client.impl.json.GsonFactory;
import com.devicehive.client.impl.json.strategies.JsonPolicyDef;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.devicehive.client.model.exceptions.HiveException;
import com.devicehive.client.model.exceptions.HiveServerException;
import com.devicehive.client.model.exceptions.InternalHiveClientException;
import com.devicehive.client.impl.util.connection.*;
import com.devicehive.client.impl.websocket.HiveClientEndpoint;
import com.devicehive.client.impl.websocket.HiveWebsocketHandler;
import com.devicehive.client.impl.websocket.SimpleWebsocketResponse;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
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
public class HiveWebSocketClient implements AutoCloseable {

    private static Logger logger = LoggerFactory.getLogger(HiveWebSocketClient.class);

    private static final String REQUEST_ID_MEMBER = "requestId";
    private static final Long WAIT_TIMEOUT = 1L;

    private static final WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();

    private final HiveContext hiveContext;
    private final URI uri;
    private final ConcurrentMap<String, SettableFuture<JsonObject>> websocketResponsesMap = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

    private Session currentSession;

    /**
     * Creates client connected to the given websocket URL. All state is kept in the hive context.
     *
     * @param uri      URI of websocket service
     * @param hiveContext context. Keeps state, for example credentials.
     */
    public HiveWebSocketClient(URI uri, HiveContext hiveContext) throws HiveException {
        this.hiveContext = hiveContext;
        this.uri = uri;
        initSession();
    }





    private synchronized void initSession() throws HiveException {
        try {
            lock.writeLock().lock();
            HiveClientEndpoint endpoint = new HiveClientEndpoint(hiveContext);
            currentSession = webSocketContainer.connectToServer(endpoint, ClientEndpointConfig.Builder.create().build(), uri);
            if (!websocketResponsesMap.isEmpty()) {
                logger.error("There are requests without responses, they will be lost in new session");
                websocketResponsesMap.clear();
            }
            currentSession.addMessageHandler(new HiveWebsocketHandler(hiveContext, websocketResponsesMap));
        } catch (DeploymentException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new HiveException("Can not connect to " + uri);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Sends message to server
     *
     * @param message some HiveEntity object in JSON
     */
    public void sendMessage(JsonObject message) throws HiveException {
        try {
            lock.readLock().lock();
            String requestId = rawSend(message);
            websocketResponsesMap.put(requestId, SettableFuture.<JsonObject>create());
            processResponse(requestId);
        } finally {
            lock.readLock().unlock();
        }
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
        try {
            lock.readLock().lock();
            String requestId = rawSend(message);
            websocketResponsesMap.put(requestId, SettableFuture.<JsonObject>create());
            return processResponse(requestId, responseMemberName, typeOfResponse, policy);
        } finally {
            lock.readLock().unlock();
        }

    }

    private String rawSend(JsonObject message) {
        String requestId = UUID.randomUUID().toString();
        message.addProperty(REQUEST_ID_MEMBER, requestId);
        currentSession.getAsyncRemote().sendObject(message);
        return requestId;
    }

    /**
     * Implementation of close method in closeable interface. Closes endpoint.
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        currentSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, ""));
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
                    throw new HiveServerException("Wrong type of response!", 500);
                }
                if (response.getStatus().equals("success")) {
                    logger.debug("Request with id:" + requestId + "proceed successfully");
                    return;
                }
                Family errorFamily = Family.familyOf(response.getCode());
                switch (errorFamily) {
                    case SERVER_ERROR:
                        logger.warn(
                                "Request id: " + requestId + ". Error message:" + response.getError() + ". Status code:"
                                        + response.getCode());
                        throw new HiveServerException(response.getError(), response.getCode());
                    case CLIENT_ERROR:
                        logger.warn(
                                "Request id: " + requestId + ". Error message:" + response.getError() + ". Status code:"
                                        + response.getCode());
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
                if (result.get("status").getAsString().equals("success")) {
                    logger.debug("Request with id:" + requestId + "proceed successfully");
                } else {
                    Family errorFamily = Family.familyOf(result.get("code").getAsInt());
                    String error = null;
                    if (result.get("error") instanceof JsonPrimitive)
                        error = result.get("error").getAsString();
                    Integer code = null;
                    if (result.get("code") instanceof JsonPrimitive)
                        result.get("code").getAsInt();
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
        throw new HiveServerException("Server does not respond!", SERVICE_UNAVAILABLE.getStatusCode());
    }
}
