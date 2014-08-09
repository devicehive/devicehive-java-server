package com.devicehive.messaging;

import com.devicehive.client.HiveClient;
import com.devicehive.client.HiveFactory;
import com.devicehive.client.model.AccessKey;
import com.devicehive.client.model.Device;
import com.devicehive.client.model.DeviceClass;
import com.devicehive.client.model.Network;
import com.devicehive.client.model.exceptions.HiveException;

import java.net.URI;
import java.util.List;

/**
 * Created by stas on 09.08.14.
 */
public class Main {

    private static final URI uri = URI.create("http://localhost:8080/dx/rest");

    public static void main(String... args) throws HiveException {
        HiveClient hiveClient = null;
        AdminTool adminTool = null;
        try {
            hiveClient = HiveFactory.createClient(uri, false);
            hiveClient.authenticate("dhadmin", "dhadmin_#911");
            adminTool = new AdminTool(hiveClient);
            adminTool.cleanup();
            List<Device> devices = adminTool.prepareTestDevices(10);
            List<AccessKey> keys = adminTool.prepareKeys(devices);
            adminTool.cleanup();
        } finally {
            if (hiveClient != null) {
                hiveClient.close();
            }
        }
    }

}
