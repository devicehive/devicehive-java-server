package com.devicehive.client.context;


import com.devicehive.client.config.Preferences;
import com.devicehive.client.json.GsonFactory;
import com.devicehive.client.model.ApiInfo;
import com.devicehive.client.model.CredentialsStorage;
import com.devicehive.client.model.Device;
import com.devicehive.client.model.Transport;
import com.devicehive.client.rest.ResponseFactory;
import com.devicehive.client.rest.controller.DeviceController;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.WebTarget;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

public class HiveContext implements Closeable {

    private final Transport transport;
    private HiveRestClient hiveRestClient;

    private HivePrincipal hivePrincipal;



    public HiveContext(Transport transport, URI rest) {
        this.transport = transport;
        hiveRestClient = new HiveRestClient(rest, this);
    }

    @Override
    public void close() throws IOException {
        hiveRestClient.close();
    }

    public synchronized HivePrincipal getHivePrincipal() {
        return hivePrincipal;
    }

    private synchronized void setHivePrincipal(HivePrincipal hivePrincipal) {
        if (this.hivePrincipal != null) {
            throw new IllegalStateException("Principal is alreay set");
        }
        this.hivePrincipal = hivePrincipal;
    }

    public synchronized ApiInfo getInfo() {
        return hiveRestClient.execute("/info", HttpMethod.GET, ApiInfo.class, null);
    }

    public static void main(String... args) {
        HiveContext hiveContext = new HiveContext(Transport.AUTO, URI.create("http://localhost:8080/hive/rest/"));
        ApiInfo apiInfo = hiveContext.getInfo();
        System.out.println(GsonFactory.createGson().toJson(apiInfo));
    }

}
