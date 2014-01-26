package com.devicehive.client.impl.context;


import com.devicehive.client.impl.rest.subs.RestSubManager;
import com.devicehive.client.impl.rest.subs.WebsocketSubManager;
import com.devicehive.client.model.*;
import com.devicehive.client.model.exceptions.HiveException;
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
import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static com.devicehive.client.impl.context.Constants.REQUIRED_VERSION_OF_API;

/**
 * Entity that keeps all state, i.e. rest and websocket client, subscriptions info, transport to use.
 */
public class HiveContext implements AutoCloseable {
    private static Logger logger = LoggerFactory.getLogger(HiveContext.class);
    private final HiveRestClient hiveRestClient;
    private final HiveWebSocketClient hiveWebSocketClient;
    private final BlockingQueue<Pair<String, DeviceCommand>> commandQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<DeviceCommand> commandUpdateQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Pair<String, DeviceNotification>> notificationQueue = new LinkedBlockingQueue<>();
    private final RestSubManager restSubManager;
    private final WebsocketSubManager websocketSubManager;


    private HivePrincipal hivePrincipal;

    /**
     * Constructor. Creates rest client or websocket client based on specified transport. If this transport is not
     * available and it is not REST_ONLY switches to another one.
     *
     * @param useWebsockets
     * @param rest                          RESTful service URL
     * @param role                          auth. level
     * @param connectionEstablishedNotifier notifier for successful reconnection completion
     * @param connectionLostNotifier        notifier for lost connection
     */
    public HiveContext(boolean useWebsockets, URI rest, Role role, ConnectionEstablishedNotifier
            connectionEstablishedNotifier, ConnectionLostNotifier connectionLostNotifier) throws HiveException {

        try {

            this.hiveRestClient = new HiveRestClient(rest, this, connectionEstablishedNotifier, connectionLostNotifier);
            ApiInfo info = hiveRestClient.execute("/info", HttpMethod.GET, null, ApiInfo.class, null);
            if (!info.getApiVersion().equals(REQUIRED_VERSION_OF_API)) {
                throw new InternalHiveClientException("incompatible version of device hive server API!");
            }

            URI websocket = websocketUriBuilder(info.getWebSocketServerUrl(), role);
            this.hiveWebSocketClient = useWebsockets ? new HiveWebSocketClient(websocket, this) : null;

            restSubManager = new RestSubManager(this);

            websocketSubManager = new WebsocketSubManager(this);
        } catch (HiveException ex) {
            close();
            throw ex;
        } catch (Exception ex) {
            close();
            throw new HiveException("Error creating Hive Context", ex);
        }


    }

    /**
     * @return true if websocket transport is available and should be used, false otherwise
     */
    public boolean isWebsocketSupported() {
        return hiveWebSocketClient != null;
    }

    /**
     * Implementation of close method in Closeable interface. Kills all subscriptions tasks and rest and websocket
     * clients.
     *
     * @throws IOException
     */
    @Override
    public synchronized void close() {
        try {
            if (websocketSubManager != null) {
                websocketSubManager.close();
            }
        } catch (Exception ex) {
            logger.error("Error closing Websocket subscriptions", ex);
        }
        try {
            if (restSubManager != null) {
                restSubManager.close();
            }
        } catch (Exception ex) {
            logger.error("Error closing REST subscriptions", ex);
        }
        try {
            if (hiveWebSocketClient != null) {
                restSubManager.close();
            }
        } catch (Exception ex) {
            logger.error("Error closing Websocket client", ex);
        }
        try {
            if (hiveRestClient != null) {
                hiveRestClient.close();
            }
        } catch (Exception ex) {
            logger.error("Error closing REST client", ex);
        }
    }

    public RestSubManager getRestSubManager() {
        return restSubManager;
    }

    public WebsocketSubManager getWebsocketSubManager() {
        return websocketSubManager;
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
    public ApiInfo getInfo() throws HiveException {
        String restUrl = null;
        if (isWebsocketSupported()) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "server/info");
            String requestId = UUID.randomUUID().toString();
            request.addProperty("requestId", requestId);
            ApiInfo apiInfo = this.hiveWebSocketClient.sendMessage(request, "info", ApiInfo.class, null);
            restUrl = apiInfo.getRestServerUrl();
        }
        ApiInfo apiInfo = hiveRestClient.execute("/info", HttpMethod.GET, null, ApiInfo.class, null);
        apiInfo.setRestServerUrl(restUrl);
        return apiInfo;
    }

    public Timestamp getServerTimestamp() throws HiveException {
        ApiInfo apiInfo = hiveRestClient.execute("/info", HttpMethod.GET, null, ApiInfo.class, null);
        return apiInfo.getServerTimestamp();
    }

    public String getServerApiVersion() throws HiveException {
        ApiInfo apiInfo = hiveRestClient.execute("/info", HttpMethod.GET, null, ApiInfo.class, null);
        return apiInfo.getApiVersion();
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
        return URI.create(StringUtils.removeEnd(websocket, "/") + role.getWebsocketSubPath());
    }
}
