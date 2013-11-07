package com.devicehive.client.api;


import com.devicehive.client.context.HiveContext;
import com.devicehive.client.context.HivePrincipal;
import com.devicehive.client.json.GsonFactory;
import com.devicehive.client.json.adapters.TimestampAdapter;
import com.devicehive.client.model.*;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.devicehive.client.util.HiveValidator;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.*;

public class SingleHiveDevice implements Closeable {
    private static Logger logger = LoggerFactory.getLogger(SingleHiveDevice.class);
    private HiveContext hiveContext;

    public SingleHiveDevice(URI restUri, URI websocketUri) {
        this.hiveContext = new HiveContext(Transport.AUTO, restUri, websocketUri);
    }

    public SingleHiveDevice(URI restUri, URI websocketUri, Transport transport) {
        this.hiveContext = new HiveContext(transport, restUri, websocketUri);
    }

    public static void main(String... args) {

        URI restUri = URI.create("http://127.0.0.1:8080/hive/rest/");
        URI websocketUri = URI.create("ws://127.0.0.1:8080/hive/websocket/");
        final SingleHiveDevice shd = new SingleHiveDevice(restUri, websocketUri, Transport.PREFER_WEBSOCKET);
        try {
            shd.authenticate("e50d6085-2aba-48e9-b1c3-73c673e414be", "05F94BF509C8");
            Device device = shd.getDevice();
            logger.debug("Received device: {}", device);
            shd.saveDevice(device);
            try {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
                Date startDate = formatter.parse("2013-10-11 13:12:00");
                shd.subscribeForCommands(new Timestamp(startDate.getTime()), null);
            } catch (ParseException e) {
                logger.error(e.getMessage(), e);
            }
            try {
                Thread.currentThread().join(5_000);
                shd.unsubscribeFromCommands(null);
                Thread.currentThread().join(5_000);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        } finally {
            try {
                shd.close();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
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
        }
        hiveContext.setHivePrincipal(HivePrincipal.createDevice(deviceId, deviceKey));
    }

    public Device getDevice() {
        if (hiveContext.useSockets()) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "device/get");
            String requestId = UUID.randomUUID().toString();
            request.addProperty("requestId", requestId);
            return hiveContext.getHiveWebSocketClient().sendMessage(request, "device", Device.class,
                    DEVICE_PUBLISHED_DEVICE_AUTH);
        } else {
            String deviceId = hiveContext.getHivePrincipal().getDevice().getKey();
            if (deviceId == null) {
                throw new HiveClientException("Device is not authenticated");
            }
            String path = "/device/" + deviceId;
            return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, Device.class, null);
        }
    }

    public void saveDevice(Device device) {
        if (hiveContext.useSockets()) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "device/save");
            String requestId = UUID.randomUUID().toString();
            request.addProperty("requestId", requestId);
            Gson gson = GsonFactory.createGson();
            request.add("device", gson.toJsonTree(device));
            Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
            request.addProperty("deviceId", authenticated.getLeft());
            request.addProperty("deviceKey", authenticated.getRight());
            hiveContext.getHiveWebSocketClient().sendMessage(request);
        } else {
            device.setKey("05F94BF509C8");
            HiveValidator.validate(device);
            String path = "/device/" + device.getId();
            hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, null, device, null);
        }
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
        if (hiveContext.useSockets()) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "command/update");
            String requestId = UUID.randomUUID().toString();
            request.addProperty("requestId", requestId);
            request.addProperty("commandId", deviceCommand.getId());
            Gson gson = GsonFactory.createGson(COMMAND_UPDATE_FROM_DEVICE);
            request.add("command", gson.toJsonTree(deviceCommand));
            hiveContext.getHiveWebSocketClient().sendMessage(request);
        } else {
            Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
            String path = "/device/" + authenticated.getKey() + "/command/" + deviceCommand.getId();
            hiveContext.getHiveRestClient()
                    .execute(path, HttpMethod.PUT, null, deviceCommand, COMMAND_UPDATE_FROM_DEVICE);
        }
    }

    public void subscribeForCommands(final Timestamp timestamp, final Set<String> names) {
        if (hiveContext.useSockets()) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "command/subscribe");
            String requestId = UUID.randomUUID().toString();
            request.addProperty("requestId", requestId);
            request.addProperty("timestamp", TimestampAdapter.formatTimestamp(timestamp));
            hiveContext.getHiveWebSocketClient().sendMessage(request);
        } else {
            Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
            final String path = "/device/" + authenticated.getKey() + "/command/poll";
            hiveContext.getHiveSubscriptions().addCommandsSubscription(null, timestamp, names,
                    authenticated.getLeft());
        }
    }

    public void unsubscribeFromCommands(final Set<String> names) {
        if (hiveContext.useSockets()) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "command/unsubscribe");
            String requestId = UUID.randomUUID().toString();
            request.addProperty("requestId", requestId);
            hiveContext.getHiveWebSocketClient().sendMessage(request);
        } else {
            Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
            hiveContext.getHiveSubscriptions().removeCommandSubscription(names, authenticated.getLeft());
        }
    }

    public DeviceNotification insertNotification(DeviceNotification deviceNotification) {
        if (hiveContext.useSockets()) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "notification/insert");
            String requestId = UUID.randomUUID().toString();
            request.addProperty("requestId", requestId);
            Gson gson = GsonFactory.createGson(NOTIFICATION_FROM_DEVICE);
            request.add("notification", gson.toJsonTree(deviceNotification));
            return hiveContext.getHiveWebSocketClient().sendMessage(request, "notification", DeviceNotification.class,
                    NOTIFICATION_TO_DEVICE);
        } else {
            Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
            HiveValidator.validate(deviceNotification);
            String path = "/device/" + authenticated.getKey() + "notification";
            return hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, null, null, deviceNotification,
                    DeviceNotification.class, NOTIFICATION_FROM_DEVICE, NOTIFICATION_TO_DEVICE);
        }
    }

    public ApiInfo getInfo() {
        return hiveContext.getInfo();
    }

}
