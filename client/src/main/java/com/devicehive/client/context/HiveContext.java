package com.devicehive.client.context;


import com.devicehive.client.json.GsonFactory;
import com.devicehive.client.model.ApiInfo;
import com.devicehive.client.model.Transport;

import javax.ws.rs.HttpMethod;
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

    public HiveRestClient getHiveRestClient() {
        return hiveRestClient;
    }

    public synchronized HivePrincipal getHivePrincipal() {
        return hivePrincipal;
    }

    public synchronized void setHivePrincipal(HivePrincipal hivePrincipal) {
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
