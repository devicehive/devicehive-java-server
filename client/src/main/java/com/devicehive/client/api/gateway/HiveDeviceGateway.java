package com.devicehive.client.api.gateway;


import com.devicehive.client.api.SubscriptionsService;
import com.devicehive.client.config.Constants;
import com.devicehive.client.context.HiveContext;
import com.devicehive.client.json.GsonFactory;
import com.devicehive.client.model.*;
import com.devicehive.client.util.HiveValidator;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.ws.rs.HttpMethod;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.util.*;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.*;

/**
 * HiveDeviceGateway is useful for communications with devices with can understand only binary protocol.
 * That scenario is common for gateways which typically proxy multiple devices and use a single connection to the server.
 */
public class HiveDeviceGateway implements Closeable {

    private static final String DEVICE_ENDPOINT_PATH = "/device";
    private HiveContext hiveContext;

    /**
     * Creates new device gateway, that can communicate with the server via provided URLs.
     *
     * @param restUri      RESTful service URL
     * @param websocketUri websocket service URL (not the URL of the device endpoint!)
     */
    public HiveDeviceGateway(URI restUri, URI websocketUri) {
        String ws = StringUtils.removeEnd(websocketUri.toString(), "/");
        this.hiveContext = new HiveContext(Transport.AUTO, restUri, URI.create(ws + DEVICE_ENDPOINT_PATH));
    }

    /**
     * Creates new device gateway, that can communicate with the server via provided URLs.
     *
     * @param restUri      RESTful service URL
     * @param websocketUri websocket service URL (not the URL of the device endpoint!)
     * @param transport    transport to use
     */
    public HiveDeviceGateway(URI restUri, URI websocketUri, Transport transport) {
        String ws = StringUtils.removeEnd(websocketUri.toString(), "/");
        this.hiveContext = new HiveContext(transport, restUri, URI.create(ws + DEVICE_ENDPOINT_PATH));
    }

    /**
     * Closes device gateway with associated context
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        hiveContext.close();
    }

    /**
     * Gets information about the requested device.
     *
     * @return current device info
     */
    public Device getDevice(String deviceId, String key) {
        if (hiveContext.useSockets()) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "device/get");
            String requestId = UUID.randomUUID().toString();
            request.addProperty("deviceId", deviceId);
            request.addProperty("deviceKey", key);
            request.addProperty("requestId", requestId);
            return hiveContext.getHiveWebSocketClient().sendMessage(request, "device", Device.class,
                    DEVICE_PUBLISHED_DEVICE_AUTH);
        } else {
            Map<String, String> headers = getHeaders(deviceId, key);
            String path = "/device/" + deviceId;
            return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, headers, Device.class, null);
        }
    }

    /**
     * Registers or updates a device.
     *
     * @param device   update/create device info
     * @param deviceId device identifier
     * @param key      device key
     */
    public void saveDevice(String deviceId, String key, Device device) {
        if (hiveContext.useSockets()) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "device/save");
            String requestId = UUID.randomUUID().toString();
            request.addProperty("requestId", requestId);
            Gson gson = GsonFactory.createGson();
            request.add("device", gson.toJsonTree(device));
            request.addProperty("deviceId", deviceId);
            request.addProperty("deviceKey", key);
            hiveContext.getHiveWebSocketClient().sendMessage(request);
        } else {
            Map<String, String> headers = getHeaders(deviceId, key);
            device.setId(deviceId);
            device.setKey(key);
            HiveValidator.validate(device);
            String path = "/device/" + device.getId();
            hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, headers, device, null);
        }
    }

    /**
     * Queries device commands.
     *
     * @param deviceId device identifier
     * @param key      device key
     * @param start    command start timestamp (UTC).
     * @param end      command end timestamp (UTC).
     * @param command  command name.
     * @param status   command status.
     * @param sortBy   Result list sort field. Available values are Timestamp (default), Command and Status.
     * @param sortAsc  if true - ascending sort order in the result list will be used, descending otherwise
     * @param take     Number of records to take from the result list (default is 1000).
     * @param skip     Number of records to skip from the result list.
     * @return list of device commands
     */
    public List<DeviceCommand> queryCommands(String deviceId, String key, Timestamp start, Timestamp end,
                                             String command, String status, String sortBy, boolean sortAsc,
                                             Integer take, Integer skip) {
        String path = "/device/" + deviceId + "/command";
        Map<String, String> headers = getHeaders(deviceId, key);
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
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, headers, queryParams,
                new TypeToken<List<DeviceCommand>>() {
                }.getType(), null);
    }

    /**
     * Gets information about device command.
     *
     * @param deviceId  device identifier
     * @param key       device key
     * @param commandId command identifier
     * @return existing device command
     */
    public DeviceCommand getCommand(String deviceId, String key, long commandId) {
        Map<String, String> headers = getHeaders(deviceId, key);
        String path = "/device/" + deviceId + "/command/" + commandId;
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, headers, DeviceCommand.class, null);
    }

    /**
     * Updates an existing device command.
     *
     * @param deviceId      device identifier
     * @param key           device key
     * @param deviceCommand update info in the device command representation
     */
    public void updateCommand(String deviceId, String key, DeviceCommand deviceCommand) {
        if (hiveContext.useSockets()) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "command/update");
            String requestId = UUID.randomUUID().toString();
            request.addProperty("requestId", requestId);
            request.addProperty("deviceId", deviceId);
            request.addProperty("deviceKey", key);
            request.addProperty("commandId", deviceCommand.getId());
            Gson gson = GsonFactory.createGson(COMMAND_UPDATE_FROM_DEVICE);
            request.add("command", gson.toJsonTree(deviceCommand));
            hiveContext.getHiveWebSocketClient().sendMessage(request);
        } else {
            Map<String, String> headers = getHeaders(deviceId, key);
            String path = "/device/" + key + "/command/" + deviceCommand.getId();
            hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, headers, deviceCommand,
                    COMMAND_UPDATE_FROM_DEVICE);
        }

    }

    /**
     * Subscribes the device to commands. After subscription is completed, the server will start to send commands to
     * the connected device.
     *
     * @param deviceId  device identifier
     * @param key       device key
     * @param timestamp Timestamp of the last received command (UTC). If not specified, the server's timestamp is taken instead.
     */
    public void subscribeForCommands(String deviceId, String key, Timestamp timestamp) {
        if (hiveContext.useSockets()) {
            SubscriptionsService.subscribeDeviceForCommands(hiveContext, timestamp, deviceId, key);
        } else {
            final Map<String, String> headers = getHeaders(deviceId, key);
            hiveContext.getHiveSubscriptions().addCommandsSubscription(headers, timestamp, null, deviceId);
        }
    }

    /**
     * Unsubscribes the device from commands.
     *
     * @param deviceId device identifier
     * @param key      device key
     */
    public void unsubscribeFromCommands(String deviceId, String key) {
        if (hiveContext.useSockets()) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "command/unsubscribe");
            String requestId = UUID.randomUUID().toString();
            request.addProperty("requestId", requestId);
            request.addProperty("deviceId", deviceId);
            request.addProperty("deviceKey", key);
            hiveContext.getHiveWebSocketClient().sendMessage(request);
            hiveContext.getHiveSubscriptions().removeWsCommandSubscription(null, deviceId);
        } else {
            Device device = getDevice(deviceId, key);
            if (device != null) {
                hiveContext.getHiveSubscriptions().removeCommandSubscription(null, deviceId);
            }
        }
    }

    /**
     * Creates new device notification on behalf of device.
     *
     * @param deviceId           device identifier
     * @param key                device key
     * @param deviceNotification device notification that should be created
     * @return info about inserted notification
     */
    public DeviceNotification insertNotification(String deviceId, String key, DeviceNotification deviceNotification) {
        if (hiveContext.useSockets()) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "notification/insert");
            String requestId = UUID.randomUUID().toString();
            request.addProperty("requestId", requestId);
            request.addProperty("deviceId", deviceId);
            request.addProperty("deviceKey", key);
            Gson gson = GsonFactory.createGson(NOTIFICATION_FROM_DEVICE);
            request.add("notification", gson.toJsonTree(deviceNotification));
            return hiveContext.getHiveWebSocketClient().sendMessage(request, "notification", DeviceNotification.class,
                    NOTIFICATION_TO_DEVICE);
        } else {
            Map<String, String> headers = getHeaders(deviceId, key);
            HiveValidator.validate(deviceNotification);
            String path = "/device/" + deviceId + "/notification";
            return hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, headers, null, deviceNotification,
                    DeviceNotification.class, NOTIFICATION_FROM_DEVICE, NOTIFICATION_TO_DEVICE);
        }

    }

    private Map<String, String> getHeaders(String deviceId, String key) {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.DEVICE_ID_HEADER, deviceId);
        headers.put(Constants.DEVICE_KEY_HEADER, key);
        return headers;
    }

    /**
     * Requests API info from server
     *
     * @return API info
     */
    public ApiInfo getInfo() {
        return hiveContext.getInfo();
    }

    /**
     * Get commands queue
     *
     * @return commands queue
     */
    public Queue<Pair<String, DeviceCommand>> getCommandsQueue() {
        return hiveContext.getCommandQueue();
    }
}
