package com.devicehive.client.context;


import com.devicehive.client.model.ApiInfo;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.Transport;
import org.apache.log4j.Logger;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

public class HiveContext implements Closeable {


    private static Logger logger = Logger.getLogger(HiveContext.class);
    private final Transport transport;
    private HiveRestClient hiveRestClient;
    private HiveWebSocketClient hiveWebSocketClient;
    private HivePrincipal hivePrincipal;

    private Map<String, Future<Response>> websocketResponsesMap = new HashMap<>();

    private HiveSubscriptions hiveSubscriptions;
    private BlockingQueue<DeviceCommand> commandQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<DeviceCommand> commandUpdateQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<DeviceNotification> notificationQueue = new LinkedBlockingQueue<>();

    public HiveContext(Transport transport, URI rest) {
        this.transport = transport;
        hiveRestClient = new HiveRestClient(rest, this);
        hiveSubscriptions = new HiveSubscriptions(this);
    }

    @Override
    public synchronized void close() throws IOException {
        try {
            hiveSubscriptions.shutdownThreads();
        } finally {
            hiveRestClient.close();
        }
    }

    public HiveSubscriptions getHiveSubscriptions(){
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




}
