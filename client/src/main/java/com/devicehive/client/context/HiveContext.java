package com.devicehive.client.context;


import com.devicehive.client.model.ApiInfo;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.Transport;
import com.devicehive.client.model.exceptions.InternalHiveClientException;
import com.google.gson.JsonObject;
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

public class HiveContext implements Closeable {


    private static Logger logger = LoggerFactory.getLogger(HiveContext.class);
    private final Transport transport;
    private HiveRestClient hiveRestClient;
    private HiveWebSocketClient hiveWebSocketClient;
    private HivePrincipal hivePrincipal;
    private HiveSubscriptions hiveSubscriptions;
    private BlockingQueue<Pair<String, DeviceCommand>> commandQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<DeviceCommand> commandUpdateQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<Pair<String, DeviceNotification>> notificationQueue = new LinkedBlockingQueue<>();

    public HiveContext(Transport transport, URI rest, URI websocket) {
        this.transport = transport;
        try {
            hiveRestClient = new HiveRestClient(rest, this);
        } catch (Exception e) {
            if (!transport.equals(Transport.REST_ONLY)) {
                logger.warn("Unable to connect to server vis REST. Some services are unavailable.");
            } else {
                throw new InternalHiveClientException("Unable to connect to server via REST", e);
            }
        }
        hiveSubscriptions = new HiveSubscriptions(this);
        if (useSockets() || hiveRestClient == null)
            try {
                hiveWebSocketClient = new HiveWebSocketClient(websocket, this);
            } catch (Exception e) {
                if (hiveRestClient != null) {
                    logger.warn("Unable connect to server via websocket. Will use REST");
                } else {
                    throw new InternalHiveClientException("Unable to connect to server!", e);
                }
            }
    }

    public boolean useSockets() {
        return transport.getWebsocketPriority() > transport.getRestPriority();
    }

    @Override
    public synchronized void close() throws IOException {
        try {
            hiveSubscriptions.shutdownThreads();
        } finally {
            hiveRestClient.close();
            if (!transport.equals(Transport.REST_ONLY))
                hiveWebSocketClient.close();
        }
    }

    public HiveSubscriptions getHiveSubscriptions() {
        return hiveSubscriptions;
    }

    public HiveRestClient getHiveRestClient() {
        return hiveRestClient;
    }

    public synchronized HivePrincipal getHivePrincipal() {
        return hivePrincipal;
    }

    public synchronized void setHivePrincipal(HivePrincipal hivePrincipal) {
        if (this.hivePrincipal != null) {
            throw new IllegalStateException("Principal is already set");
        }
        this.hivePrincipal = hivePrincipal;
    }

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

    public BlockingQueue<Pair<String, DeviceCommand>> getCommandQueue() {
        return commandQueue;
    }

    public BlockingQueue<DeviceCommand> getCommandUpdateQueue() {
        return commandUpdateQueue;
    }

    public BlockingQueue<Pair<String, DeviceNotification>> getNotificationQueue() {
        return notificationQueue;
    }

    public HiveWebSocketClient getHiveWebSocketClient() {
        return hiveWebSocketClient;
    }
}
