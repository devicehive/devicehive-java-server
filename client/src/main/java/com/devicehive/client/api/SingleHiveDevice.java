package com.devicehive.client.api;


import com.devicehive.client.context.HiveContext;
import com.devicehive.client.context.HivePrincipal;
import com.devicehive.client.model.*;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.devicehive.client.util.HiveValidator;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import javax.ws.rs.HttpMethod;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.util.*;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.*;

public class SingleHiveDevice implements Closeable {
    private static Logger logger = Logger.getLogger(SingleHiveDevice.class);
    private HiveContext hiveContext;

    public SingleHiveDevice(URI restUri, URI websocketUri) {
        this.hiveContext = new HiveContext(Transport.AUTO, restUri, websocketUri);
    }

    public SingleHiveDevice(URI restUri, URI websocketUri,  Transport transport) {
        this.hiveContext = new HiveContext(transport, restUri, websocketUri);
    }

    public static void main(String... args) {
        URI restUri = URI.create("http://127.0.0.1:8080/hive/rest/");
        URI websocketUri =  URI.create("ws://127.0.0.1:8080/hive/websocket/");
        final SingleHiveDevice shd = new SingleHiveDevice(restUri, websocketUri, Transport.PREFER_WEBSOCKET);
        shd.authenticate("e50d6085-2aba-48e9-b1c3-73c673e414be", "05F94BF509C8");
//        try {
//            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
//            Date startDate = formatter.parse("2013-10-11 13:12:00");
//            shd.subscribeForCommands(new Timestamp(startDate.getTime()), null);
//        } catch (ParseException e) {
//            logger.error(e);
//        }
        try {
            Thread.currentThread().join(5_000);
//            shd.unsubscribeFromCommands(null);
//            Thread.currentThread().join(300_000);
            shd.close();
        } catch (InterruptedException | IOException e) {
            logger.error(e);
        }
    }

    @Override
    public void close() throws IOException {
        hiveContext.close();
    }

    public void authenticate(String deviceId, String deviceKey) {
        if (hiveContext.useSockets()) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "authenticate");
            String requestId = UUID.randomUUID().toString();
            request.addProperty("requestId", requestId);
            request.addProperty("deviceId", deviceId);
            request.addProperty("deviceKey", deviceKey);
            hiveContext.getHiveWebSocketClient().sendMessage(request);
        } else {
            hiveContext.setHivePrincipal(HivePrincipal.createDevice(deviceId, deviceKey));
        }
    }

    public Device getDevice() {
        String deviceId = hiveContext.getHivePrincipal().getDevice().getKey();
        if (deviceId == null) {
            throw new HiveClientException("Device is not authenticated");
        }
        String path = "/device/" + deviceId;
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, Device.class, null);
    }

    public void saveDevice(Device device) {
        Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
        device.setKey("05F94BF509C8");
        HiveValidator.validate(device);
        String path = "/device/" + device.getId();
        hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, null, device, null);
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
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, queryParams,
                new TypeToken<List<DeviceCommand>>() {
                }.getType(), null);
    }

    public DeviceCommand getCommand(long commandId) {
        Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
        String path = "/device/" + authenticated.getKey() + "/command/" + commandId;
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, DeviceCommand.class, null);
    }

    public void updateCommand(DeviceCommand deviceCommand) {
        Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
        String path = "/device/" + authenticated.getKey() + "/command/" + deviceCommand.getId();
        hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, null, deviceCommand, COMMAND_UPDATE_FROM_DEVICE);
    }

    public void subscribeForCommands(final Timestamp timestamp, final Set<String> names) {
        Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
        final String path = "/device/" + authenticated.getKey() + "/command/poll";
        hiveContext.getHiveSubscriptions().addCommandsSubscription(null, timestamp, names, authenticated.getLeft());
    }

    public void unsubscribeFromCommands(final Set<String> names) {
        Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
        hiveContext.getHiveSubscriptions().removeCommandSubscription(names, authenticated.getLeft());
    }

    public DeviceNotification insertNotification(DeviceNotification deviceNotification) {
        Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
        HiveValidator.validate(deviceNotification);
        String path = "/device/" + authenticated.getKey() + "notification";
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, null, null, deviceNotification,
                DeviceNotification.class, NOTIFICATION_FROM_DEVICE, NOTIFICATION_TO_DEVICE);
    }

    public ApiInfo getInfo() {
        return hiveContext.getInfo();
    }

}
