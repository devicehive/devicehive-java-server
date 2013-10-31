package com.devicehive.client.api;


import com.devicehive.client.context.HiveContext;
import com.devicehive.client.context.HivePrincipal;
import com.devicehive.client.model.AccessKey;
import com.devicehive.client.model.ApiInfo;
import com.devicehive.client.model.Transport;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.util.List;

public class Client implements HiveClient {

    private static Logger logger = Logger.getLogger(SingleHiveDevice.class);
    private final HiveContext hiveContext;

    public Client(URI uri) {
        hiveContext = new HiveContext(Transport.AUTO, uri);
    }

    public static void main(String... args) {
        HiveClient client = new Client(URI.create("http://127.0.0.1:8080/hive/rest/"));
        client.authenticate("dhadmin", "dhadmin_#911");
        AccessKeyController akc = client.getAccessKeyController();
        List<AccessKey> result = akc.listKeys(1);
        logger.debug(result);
        AccessKey toInsert = result.get(0);
        AccessKey inserted = akc.insertKey(1, toInsert);
        logger.debug(inserted.getId());
        logger.debug(inserted.getKey());
        akc.deleteKey(1, inserted.getId());
    }

    public ApiInfo getInfo() {
        return null;
    }

    public void authenticate(String login, String password) {
        hiveContext.setHivePrincipal(HivePrincipal.createUser(login, password));
    }

    public AccessKeyController getAccessKeyController() {
        return new AccessKeyControllerImpl(hiveContext);
    }

    public CommandsController getCommandsController() {
        return null;
    }

    public DeviceController getDeviceController() {
        return null;
    }

    public NetworkContorller getNetworkController() {
        return null;
    }

    public NotificationsController getNotificationsController() {
        return null;
    }

    public UserContorller getUserController() {
        return null;
    }

    @Override
    public void close() throws IOException {
        try {
        } finally {
            hiveContext.close();
        }
    }

}
