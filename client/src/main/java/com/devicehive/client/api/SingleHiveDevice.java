package com.devicehive.client.api;


import com.devicehive.client.context.HiveContext;
import com.devicehive.client.context.HivePrincipal;
import com.devicehive.client.json.GsonFactory;
import com.devicehive.client.model.Device;
import com.devicehive.client.model.Transport;
import com.devicehive.client.model.exceptions.HiveClientException;

import javax.ws.rs.HttpMethod;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

public class SingleHiveDevice implements Closeable {

    private HiveContext hiveContext;

    public SingleHiveDevice(URI restUri) {
        this.hiveContext = new HiveContext(Transport.AUTO, restUri);
    }

    @Override
    public void close() throws IOException {
        hiveContext.close();
    }

    public void authenticate(String deviceId, String deviceKey) {
        hiveContext.setHivePrincipal(HivePrincipal.createDevice(deviceId, deviceKey));
    }

    public Device getDevice() {
        String deviceId = hiveContext.getHivePrincipal().getDevice().getKey();
        if (deviceId == null) {
            throw new HiveClientException("Device is not authenticated");
        }
        String path = "/device/" + deviceId;
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, Device.class, null);
    }
    /*
    public void saveDevice(Device device) {

    }

    public List<DeviceCommand> queryCommands(Timestamp start, Timestamp end, String command, String status,
                                      String sortBy, boolean sortAsc, Integer take, Integer skip) {

    }

    public DeviceCommand getCommand(long commandId) {

    }

    public void updateCommand(DeviceCommand deviceCommand) {

    }

    public void subscribeForCommands(CommandHandler handler) {

    }

    public void unsubscribeFromCommands() {

    }

    public DeviceNotification insertNotification(DeviceNotification deviceNotification) {

    } */

    public static void main(String... args) {
        SingleHiveDevice shd = new SingleHiveDevice(URI.create("http://localhost:8080/hive/rest/"));
        shd.authenticate("e50d6085-2aba-48e9-b1c3-73c673e414be", "05F94BF509C8");
        Device device = shd.getDevice();
        System.out.println(GsonFactory.createGson().toJson(device));
    }


}
