package com.devicehive.messaging;

import com.devicehive.client.HiveClient;
import com.devicehive.client.HiveFactory;
import com.devicehive.client.model.Device;
import com.devicehive.client.model.exceptions.HiveException;
import com.devicehive.messaging.config.Constants;

import java.util.List;

/**
 * Created by stas on 09.08.14.
 */
public class Main {


    public static void main(String... args) throws HiveException, InterruptedException {
        HiveClient hiveClient = null;
        AdminTool adminTool = null;
        try {
            hiveClient = HiveFactory.createClient(Constants.REST_URI, true);
            hiveClient.authenticate("dhadmin", "dhadmin_#911");
            adminTool = new AdminTool(hiveClient);
            adminTool.cleanup();
            List<Device> devices = adminTool.prepareTestDevices(Constants.MAX_DEVICES);
//            List<AccessKey> keys = adminTool.prepareKeys(devices);
            Messager msg = new Messager();
            adminTool.prepareSubscriptions(msg.getCommandsHandler(), msg.getNotificationsHandler());
            msg.startSendCommands(devices, adminTool.getTestClients());
            msg.startSendNotifications(adminTool.getTestDevices());
            Thread.currentThread().join(1_000L);
        } catch (Exception e){
            System.err.print(e);
        } finally {
            if (adminTool != null) {
                adminTool.cleanup();
            }
            if (hiveClient != null) {
                hiveClient.close();
            }
        }
    }

}
