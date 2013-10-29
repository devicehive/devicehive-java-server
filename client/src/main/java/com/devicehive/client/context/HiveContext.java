package com.devicehive.client.context;


import com.devicehive.client.model.ApiInfo;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.Transport;

import javax.ws.rs.HttpMethod;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class HiveContext implements Closeable {

    private final Transport transport;
    private HiveRestClient hiveRestClient;

    private HivePrincipal hivePrincipal;


    private BlockingQueue<DeviceCommand> commandQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<DeviceCommand> commandUpdateQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<DeviceNotification> notificationQueue = new LinkedBlockingQueue<>();

    public HiveContext(Transport transport, URI rest) {
        this.transport = transport;
        hiveRestClient = new HiveRestClient(rest, this);
    }

    @Override
    public void close() throws IOException {
        hiveRestClient.close();
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
        return hiveRestClient.execute("/info", HttpMethod.GET, ApiInfo.class, null);
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
