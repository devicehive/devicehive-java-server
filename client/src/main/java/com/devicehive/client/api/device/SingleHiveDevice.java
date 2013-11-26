package com.devicehive.client.api.device;


import com.devicehive.client.api.AuthenticationService;
import com.devicehive.client.api.SubscriptionsService;
import com.devicehive.client.context.HiveContext;
import com.devicehive.client.context.HivePrincipal;
import com.devicehive.client.json.GsonFactory;
import com.devicehive.client.model.*;
import com.devicehive.client.model.exceptions.HiveClientException;
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
 * SingleHiveDevice represents a simple device in terms of DeviceHive.
 * After connection is eshtablished, devices need to registered, perform authentication and then start sending notifications.
 * Devices may also subscribe to commands and then start receiving server-originated messages about new commands.
 */
public class SingleHiveDevice implements Closeable {
    private static final String DEVICE_ENDPOINT_PATH = "/device";
    private HiveContext hiveContext;

    /**
     * Creates new simple device, that can communicate with the server via provided URLs.
     *
     * @param restUri      RESTful service URL
     * @param websocketUri websocket service URL (not the URL of the device endpoint!)
     */
    public SingleHiveDevice(URI restUri, URI websocketUri) {
        String ws = StringUtils.removeEnd(websocketUri.toString(), "/");
        this.hiveContext = new HiveContext(Transport.AUTO, restUri, URI.create(ws + DEVICE_ENDPOINT_PATH));
    }

    /**
     * Creates new simple device, that can communicate with the server via provided URLs.
     *
     * @param restUri      RESTful service URL
     * @param websocketUri websocket service URL (not the URL of the device endpoint!)
     * @param transport    transport to use
     */
    public SingleHiveDevice(URI restUri, URI websocketUri, Transport transport) {
        String ws = StringUtils.removeEnd(websocketUri.toString(), "/");
        this.hiveContext = new HiveContext(transport, restUri, URI.create(ws + DEVICE_ENDPOINT_PATH));
    }

    /**
     * Closes single hive device with associated context
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        hiveContext.close();
    }

    /**
     * Authenticates a device.
     *
     * @param deviceId  device identifier
     * @param deviceKey device key
     */
    public void authenticate(String deviceId, String deviceKey) {
        if (hiveContext.useSockets()) {
            AuthenticationService.authenticateDevice(deviceId, deviceKey, hiveContext);
        }
        hiveContext.setHivePrincipal(HivePrincipal.createDevice(deviceId, deviceKey));
    }

    /**
     * Gets information about the current device.
     *
     * @return current device info
     */
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

    /**
     * Registers or updates a device.
     *
     * @param device update/create device info
     */
    public void registerDevice(Device device) {
        if (hiveContext.useSockets()) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "device/save");
            String requestId = UUID.randomUUID().toString();
            request.addProperty("requestId", requestId);
            Gson gson = GsonFactory.createGson();
            request.add("device", gson.toJsonTree(device));
            if (hiveContext.getHivePrincipal() != null) {
                Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
                request.addProperty("deviceId", authenticated.getLeft());
                request.addProperty("deviceKey", authenticated.getRight());
            } else {
                request.addProperty("deviceId", device.getId());
                request.addProperty("deviceKey", device.getKey());
            }
            hiveContext.getHiveWebSocketClient().sendMessage(request);
        } else {
            device.setKey("05F94BF509C8");
            HiveValidator.validate(device);
            String path = "/device/" + device.getId();
            hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, null, device, null);
        }
    }

    /**
     * Queries device commands.
     *
     * @param start   command start timestamp (UTC).
     * @param end     command end timestamp (UTC).
     * @param command command name.
     * @param status  command status.
     * @param sortBy  Result list sort field. Available values are Timestamp (default), Command and Status.
     * @param sortAsc if true - ascending sort order in the result list will be used, descending otherwise
     * @param take    Number of records to take from the result list (default is 1000).
     * @param skip    Number of records to skip from the result list.
     * @return list of device commands
     */
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

    /**
     * Gets information about device command.
     *
     * @param commandId command identifier
     * @return existing device command
     */
    public DeviceCommand getCommand(long commandId) {
        Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
        String path = "/device/" + authenticated.getKey() + "/command/" + commandId;
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, DeviceCommand.class, null);
    }

    /**
     * Updates an existing device command.
     *
     * @param deviceCommand update info in the device command representation
     */
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

    /**
     * Subscribes the device to commands. After subscription is completed, the server will start to send commands to
     * the connected device.
     *
     * @param timestamp Timestamp of the last received command (UTC). If not specified, the server's timestamp is taken instead.
     */
    public void subscribeForCommands(final Timestamp timestamp) {
        if (hiveContext.useSockets()) {
            SubscriptionsService.subscribeDeviceForCommands(hiveContext, timestamp);
        } else {
            Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
            hiveContext.getHiveSubscriptions().addCommandsSubscription(null, timestamp, null,
                    authenticated.getLeft());
        }
    }

    /**
     * Unsubscribes the device from commands.
     */
    public void unsubscribeFromCommands() {
        if (hiveContext.useSockets()) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "command/unsubscribe");
            String requestId = UUID.randomUUID().toString();
            request.addProperty("requestId", requestId);
            hiveContext.getHiveWebSocketClient().sendMessage(request);
            Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
            hiveContext.getHiveSubscriptions().removeWsCommandSubscription(null, authenticated.getLeft());
        } else {
            Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
            hiveContext.getHiveSubscriptions().removeCommandSubscription(null, authenticated.getLeft());
        }
    }

    /**
     * Creates new device notification on behalf of device.
     *
     * @param deviceNotification device notification that should be created
     * @return info about inserted notification
     */
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
            String path = "/device/" + authenticated.getKey() + "/notification";
            return hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, null, null, deviceNotification,
                    DeviceNotification.class, NOTIFICATION_FROM_DEVICE, NOTIFICATION_TO_DEVICE);
        }
    }

    /**
     * Get commands queue
     *
     * @return commands queue
     */
    public Queue<Pair<String, DeviceCommand>> getCommandsQueue() {
        return hiveContext.getCommandQueue();
    }

    /**
     * Requests API info from server
     *
     * @return API info
     */
    public ApiInfo getInfo() {
        return hiveContext.getInfo();
    }

}
