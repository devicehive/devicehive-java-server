package com.devicehive.messaging;

import com.devicehive.client.HiveClient;
import com.devicehive.client.HiveFactory;
import com.devicehive.client.model.AccessKey;
import com.devicehive.client.model.Device;
import com.devicehive.client.model.exceptions.HiveException;
import com.devicehive.messaging.config.Constants;

import java.util.List;

/**
 * Created by stas on 09.08.14.
 */
public class Main {


    public static void main(String... args) throws HiveException {
        HiveClient hiveClient = null;
        AdminTool adminTool = null;
        try {
            hiveClient = HiveFactory.createClient(Constants.REST_URI, false);
            hiveClient.authenticate("dhadmin", "dhadmin_#911");
            adminTool = new AdminTool(hiveClient);
            adminTool.cleanup();
            List<Device> devices = adminTool.prepareTestDevices(Constants.MAX_DEVICES);
            List<AccessKey> keys = adminTool.prepareKeys(devices);
            Messager msg = new Messager();
            adminTool.prepareSubscriptions(msg.getCommandsHandler(), msg.getNotificationsHandler());
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
