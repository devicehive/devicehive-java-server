package com.devicehive.client.api;


import com.devicehive.client.context.CommandHandler;
import com.devicehive.client.context.HiveContext;
import com.devicehive.client.context.HivePrincipal;
import com.devicehive.client.json.GsonFactory;
import com.devicehive.client.model.Device;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.Transport;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.devicehive.client.util.HiveValidator;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.tuple.Pair;

import javax.ws.rs.HttpMethod;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SingleHiveDevice implements Closeable {

    private HiveContext hiveContext;

    public SingleHiveDevice(URI restUri) {
        this.hiveContext = new HiveContext(Transport.AUTO, restUri);
    }

    public static void main(String... args) {
        SingleHiveDevice shd = new SingleHiveDevice(URI.create("http://127.0.0.1:8080/hive/rest/"));
        shd.authenticate("e50d6085-2aba-48e9-b1c3-73c673e414be", "05F94BF509C8");
        Device device = shd.getDevice();
        Gson gson = GsonFactory.createGson();
        JsonObject obj = (JsonObject) gson.toJsonTree(device);
        System.out.println(obj.toString());
        device.setName("changedName");
        shd.saveDevice(device);
        List<DeviceCommand> commands = shd.queryCommands(null, null, null, null, null, true, null, null);
        System.out.println(commands.toString());
        DeviceCommand command = shd.getCommand(1L);
        System.out.print(command.toString());
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

    public void saveDevice(Device device) {
        Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
        device.setKey("05F94BF509C8");
        HiveValidator.validate(device);
        String path = "/device/" + device.getId();
        hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, device, null);
    }

    public List<DeviceCommand> queryCommands(Timestamp start, Timestamp end, String command, String status,
                                             String sortBy, boolean sortAsc, Integer take, Integer skip) {
        Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
        String path = "/device/" + authenticated.getKey() + "/command";
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("start", start);
        queryParams.put("end", end);
        queryParams.put("command", command);
        queryParams.put("status", status);
        queryParams.put("sortField", sortBy);
        String order = sortAsc ? "ASC" : "DESC";
        queryParams.put("sortOrder", order);
        queryParams.put("take", take);
        queryParams.put("skip", skip);
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, queryParams,
                new TypeToken<List<DeviceCommand>>() {
                }.getType(), null);
    }

    public DeviceCommand getCommand(long commandId) {
        Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
        String path = "/device/" + authenticated.getKey() + "/command/" + commandId;
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, DeviceCommand.class, null);
    }

    public void updateCommand(DeviceCommand deviceCommand) {

    }

    public void subscribeForCommands(CommandHandler handler) {

    }

    public void unsubscribeFromCommands() {

    }

    public DeviceNotification insertNotification(DeviceNotification deviceNotification) {
        //TODO
        return null;
    }


}
