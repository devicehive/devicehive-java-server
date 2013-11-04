package com.devicehive.client.api;


import com.devicehive.client.context.HiveContext;
import com.devicehive.client.context.HivePrincipal;
import com.devicehive.client.model.AccessKey;
import com.devicehive.client.model.ApiInfo;
import com.devicehive.client.model.Device;
import com.devicehive.client.model.Transport;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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

        //access keys
        AccessKeyController akc = client.getAccessKeyController();
        List<AccessKey> result = akc.listKeys(1);
        logger.debug(result);
        AccessKey toInsert = result.get(0);
        AccessKey inserted = akc.insertKey(1, toInsert);
        logger.debug(inserted.getId());
        logger.debug(inserted.getKey());
        akc.deleteKey(1, inserted.getId());

        //devices
        DeviceController dc = client.getDeviceController();
        List<Device> deviceList =
                dc.listDevices(null, null, null, null, null, null, null, null, null, null, null, null);
        logger.debug(deviceList);

        //commands subscriptions
        CommandsController cc = client.getCommandsController();
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
            Date startDate = formatter.parse("2013-10-11 13:12:00");
            cc.subscribeForCommands(new Timestamp(startDate.getTime()), "e50d6085-2aba-48e9-b1c3-73c673e414be");
        } catch (ParseException e) {
            logger.error(e);
        }

    }

    public ApiInfo getInfo() {
        return hiveContext.getInfo();
    }

    public void authenticate(String login, String password) {
        hiveContext.setHivePrincipal(HivePrincipal.createUser(login, password));
    }

    public AccessKeyController getAccessKeyController() {
        return new AccessKeyControllerImpl(hiveContext);
    }

    public CommandsController getCommandsController() {
        return new CommandsControllerImpl(hiveContext);
    }

    public DeviceController getDeviceController() {
        return new DeviceControllerImpl(hiveContext);
    }

    public NetworkController getNetworkController() {
        return new NetworkControllerImpl(hiveContext);
    }

    public NotificationsController getNotificationsController() {
        return new NotificationsControllerImpl(hiveContext);
    }

    public UserController getUserController() {
        return new UserControllerImpl(hiveContext);
    }

    @Override
    public void close() throws IOException {
        hiveContext.close();
    }

}
