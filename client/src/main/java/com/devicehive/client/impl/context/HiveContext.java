package com.devicehive.client.impl.context;


import com.devicehive.client.model.*;
import com.devicehive.client.model.exceptions.InternalHiveClientException;
import com.devicehive.client.impl.util.connection.ConnectionEstablishedNotifier;
import com.devicehive.client.impl.util.connection.ConnectionLostNotifier;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static com.devicehive.client.impl.context.Constants.REQUIRED_VERSION_OF_API;

/**
 * Entity that keeps all state, i.e. rest and websocket client, subscriptions info, transport to use.
 */
public class HiveContext implements Closeable {
    private static final String CLIENT_ENDPOINT_PATH = "/client";
    private static final String DEVICE_ENDPOINT_PATH = "/device";
    private static Logger logger = LoggerFactory.getLogger(HiveContext.class);
    private final Transport transport;
    private HiveRestClient hiveRestClient;
    private HiveWebSocketClient hiveWebSocketClient;
    private HivePrincipal hivePrincipal;
    private HiveSubscriptions hiveSubscriptions;
    private BlockingQueue<Pair<String, DeviceCommand>> commandQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<DeviceCommand> commandUpdateQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<Pair<String, DeviceNotification>> notificationQueue = new LinkedBlockingQueue<>();

    /**
     * Constructor. Creates rest client or websocket client based on specified transport. If this transport is not
     * available and it is not REST_ONLY switches to another one.
     *
     * @param transport transport that defines protocol that should be used
     * @param role      auth. level
     * @param rest      RESTful service URL
     */
    public HiveContext(Transport transport, URI rest, Role role) {
        Transport transportToSet = transport;
        hiveRestClient = new HiveRestClient(rest, this);
        ApiInfo info = hiveRestClient.execute("/info", HttpMethod.GET, null, ApiInfo.class, null);
        if (!info.getApiVersion().equals(REQUIRED_VERSION_OF_API)) {
            throw new InternalHiveClientException("incompatible version of device hive server API!");
        }
        hiveSubscriptions = new HiveSubscriptions(this);
        if (transport.getWebsocketPriority() > transport.getRestPriority())
            try {
                URI websocket = websocketUriBuilder(info.getWebSocketServerUrl(), role);
                hiveWebSocketClient = new HiveWebSocketClient(websocket, this);
            } catch (Exception e) {
                logger.warn("Unable connect to server via websocket. Will use REST");
                transportToSet = Transport.REST_ONLY;
            }
        this.transport = transportToSet;
    }

    /**
     * Constructor. Creates rest client or websocket client based on specified transport. If this transport is not
     * available and it is not REST_ONLY switches to another one.
     *
     * @param transport                     transport that defines protocol that should be used
     * @param rest                          RESTful service URL
     * @param role                          auth. level
     * @param connectionEstablishedNotifier notifier for successful reconnection completion
     * @param connectionLostNotifier        notifier for lost connection
     */
    public HiveContext(Transport transport, URI rest, Role role, ConnectionEstablishedNotifier
            connectionEstablishedNotifier, ConnectionLostNotifier connectionLostNotifier) {
        Transport transportToSet = transport;
        hiveRestClient = new HiveRestClient(rest, this, connectionEstablishedNotifier, connectionLostNotifier);
        ApiInfo info = hiveRestClient.execute("/info", HttpMethod.GET, null, ApiInfo.class, null);
        if (!info.getApiVersion().equals(REQUIRED_VERSION_OF_API)) {
            throw new InternalHiveClientException("incompatible version of device hive server API!");
        }
        hiveSubscriptions = new HiveSubscriptions(this);
        if (transport.getWebsocketPriority() > transport.getRestPriority())
            try {
                URI websocket = websocketUriBuilder(info.getWebSocketServerUrl(), role);
                hiveWebSocketClient = new HiveWebSocketClient(websocket, this, connectionEstablishedNotifier,
                        connectionLostNotifier);
            } catch (Exception e) {
                logger.warn("Unable connect to server via websocket. Will use REST");
                transportToSet = Transport.REST_ONLY;
            }
        this.transport = transportToSet;
    }

    /**
     * Constructor. Creates rest client or websocket client based on specified transport. If this transport is not
     * available and it is not REST_ONLY switches to another one.
     *
     * @param transport              transport that defines protocol that should be used
     * @param rest                   RESTful service URL
     * @param role                   auth. level
     * @param connectionLostNotifier notifier for lost connection
     */
    public HiveContext(Transport transport, URI rest, Role role, ConnectionLostNotifier connectionLostNotifier) {
        Transport transportToSet = transport;
        hiveRestClient = new HiveRestClient(rest, this, connectionLostNotifier);
        ApiInfo info = hiveRestClient.execute("/info", HttpMethod.GET, null, ApiInfo.class, null);
        if (!info.getApiVersion().equals(REQUIRED_VERSION_OF_API)) {
            throw new InternalHiveClientException("incompatible version of device hive server API!");
        }
        hiveSubscriptions = new HiveSubscriptions(this);
        if (transport.getWebsocketPriority() > transport.getRestPriority())
            try {
                URI websocket = websocketUriBuilder(info.getWebSocketServerUrl(), role);
                hiveWebSocketClient = new HiveWebSocketClient(websocket, this, connectionLostNotifier);
            } catch (Exception e) {
                logger.warn("Unable connect to server via websocket. Will use REST");
                transportToSet = Transport.REST_ONLY;
            }
        this.transport = transportToSet;
    }

    /**
     * @return true if websocket transport is available and should be used, false otherwise
     */
    public boolean useSockets() {
        return transport.getWebsocketPriority() > transport.getRestPriority();
    }

    /**
     * Implementation of close method in Closeable interface. Kills all subscriptions tasks and rest and websocket
     * clients.
     *
     * @throws IOException
     */
    @Override
    public synchronized void close() throws IOException {
        try {
            hiveSubscriptions.shutdownThreads();
        } finally {
            if (hiveRestClient != null)
                hiveRestClient.close();
            if (hiveWebSocketClient != null)
                hiveWebSocketClient.close();
        }
    }

    /**
     * Get storage of all made subscriptions.
     *
     * @return storage of all made subscriptions.
     */
    public HiveSubscriptions getHiveSubscriptions() {
        return hiveSubscriptions;
    }

    /**
     * Get rest client.
     *
     * @return rest client.
     */
    public HiveRestClient getHiveRestClient() {
        return hiveRestClient;
    }

    /**
     * Get hive principal (credentials storage).
     *
     * @return hive principal
     */
    public synchronized HivePrincipal getHivePrincipal() {
        return hivePrincipal;
    }

    /**
     * Set hive principal if no one set yet.
     *
     * @param hivePrincipal hive principal with credentials.
     */
    public synchronized void setHivePrincipal(HivePrincipal hivePrincipal) {
        if (this.hivePrincipal != null) {
            throw new IllegalStateException("Principal is already set");
        }
        this.hivePrincipal = hivePrincipal;
    }

    /**
     * Get API info from server
     *
     * @return API info
     */
    public synchronized ApiInfo getInfo() {
        if (useSockets()) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "server/info");
            String requestId = UUID.randomUUID().toString();
            request.addProperty("requestId", requestId);
            return hiveWebSocketClient.sendMessage(request, "info", ApiInfo.class, null);
        } else {
            return hiveRestClient.execute("/info", HttpMethod.GET, null, ApiInfo.class, null);
        }
    }

    /**
     * Get commands queue.
     *
     * @return commands queue
     */
    public BlockingQueue<Pair<String, DeviceCommand>> getCommandQueue() {
        return commandQueue;
    }

    /**
     * Get command updates queue
     *
     * @return command updates queue
     */
    public BlockingQueue<DeviceCommand> getCommandUpdateQueue() {
        return commandUpdateQueue;
    }

    /**
     * Get notifications queue
     *
     * @return notifications queue
     */
    public BlockingQueue<Pair<String, DeviceNotification>> getNotificationQueue() {
        return notificationQueue;
    }

    /**
     * Get websocket client.
     *
     * @return websocket client
     */
    public HiveWebSocketClient getHiveWebSocketClient() {
        return hiveWebSocketClient;
    }

    //Private methods------------------------------------------------------------------------------------------
    private URI websocketUriBuilder(String websocket, Role role) {
        String ws = StringUtils.removeEnd(websocket, "/");
        if (role.equals(Role.USER)) {
            return URI.create(ws + CLIENT_ENDPOINT_PATH);
        } else {
            return URI.create(ws + DEVICE_ENDPOINT_PATH);
        }
    }
}
