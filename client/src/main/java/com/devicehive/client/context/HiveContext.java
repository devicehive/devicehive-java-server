package com.devicehive.client.context;


import com.devicehive.client.model.ApiInfo;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class HiveContext implements Closeable {


    private static Logger logger = LoggerFactory.getLogger(HiveContext.class);
    private final Transport transport;
    private HiveRestClient hiveRestClient;
    private HiveWebSocketClient hiveWebSocketClient;
    private HivePrincipal hivePrincipal;
    private HiveSubscriptions hiveSubscriptions;
    private BlockingQueue<DeviceCommand> commandQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<DeviceCommand> commandUpdateQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<DeviceNotification> notificationQueue = new LinkedBlockingQueue<>();

    public HiveContext(Transport transport, URI rest, URI websocket) {
        this.transport = transport;
        hiveRestClient = new HiveRestClient(rest, this);
        hiveSubscriptions = new HiveSubscriptions(this);
        hiveWebSocketClient = new HiveWebSocketClient(websocket, this);
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
        return hiveRestClient.execute("/info", HttpMethod.GET, null, ApiInfo.class, null);
    }

    public BlockingQueue<DeviceCommand> getCommandQueue() {
        return commandQueue;
    }

    public BlockingQueue<DeviceCommand> getCommandUpdateQueue() {
        return commandUpdateQueue;
    }

    public BlockingQueue<DeviceNotification> getNotificationQueue() {
        return notificationQueue;
    }

    public HiveWebSocketClient getHiveWebSocketClient() {
        return hiveWebSocketClient;
    }
}
