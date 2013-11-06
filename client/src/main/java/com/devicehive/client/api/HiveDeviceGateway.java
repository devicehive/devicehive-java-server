package com.devicehive.client.api;


import com.devicehive.client.config.Constants;
import com.devicehive.client.context.HiveContext;
import com.devicehive.client.model.*;
import com.devicehive.client.util.HiveValidator;
import com.google.common.reflect.TypeToken;
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

public class HiveDeviceGateway implements Closeable {

    private static Logger logger = LoggerFactory.getLogger(HiveDeviceGateway.class);
    private HiveContext hiveContext;

    public HiveDeviceGateway(URI restUri, URI websocketUri) {
        hiveContext = new HiveContext(Transport.AUTO, restUri, websocketUri);
    }

    public static void main(String... args) {
        URI restUri = URI.create("http://127.0.0.1:8080/hive/rest/");
        URI websocketUri =  URI.create("ws://127.0.0.1:8080/hive/websocket/");
        HiveDeviceGateway gateway = new HiveDeviceGateway(restUri, websocketUri);
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
            Date startDate = formatter.parse("2013-10-11 13:12:00");
            gateway.subscribeForCommands("e50d6085-2aba-48e9-b1c3-73c673e414be", "05F94BF509C8",
                    new Timestamp(startDate.getTime()), null);
        } catch (ParseException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void close() throws IOException {
        hiveContext.close();
    }

    public Device getDevice(String deviceId, String key) {
        Map<String, String> headers = getHeaders(deviceId, key);
        String path = "/device/" + deviceId;
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, headers, Device.class, null);
    }

    public void saveDevice(String deviceId, String key, Device device) {
        Map<String, String> headers = getHeaders(deviceId, key);
        device.setId(deviceId);
        device.setKey(key);
        HiveValidator.validate(device);
        String path = "/device/" + device.getId();
        hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, headers, device, null);

    }

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

    public DeviceCommand getCommand(String deviceId, String key, long commandId) {
        Map<String, String> headers = getHeaders(deviceId, key);
        String path = "/device/" + deviceId + "/command/" + commandId;
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, headers, DeviceCommand.class, null);
    }

    public void updateCommand(String deviceId, String key, DeviceCommand deviceCommand) {
        Map<String, String> headers = getHeaders(deviceId, key);
        String path = "/device/" + key + "/command/" + deviceCommand.getId();
        hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, headers, deviceCommand,
                COMMAND_UPDATE_FROM_DEVICE);

    }

    public void subscribeForCommands(String deviceId, String key, Timestamp timestamp, Set<String> names) {
        final Map<String, String> headers = getHeaders(deviceId, key);
        hiveContext.getHiveSubscriptions().addCommandsSubscription(headers, timestamp, names, deviceId);
    }

    public void unsubscribeFromCommands(Set<String> names, String deviceId, String key) {
        Device device = getDevice(deviceId, key);
        if (device != null) {
            hiveContext.getHiveSubscriptions().removeCommandSubscription(names, deviceId);
        }
    }

    public DeviceNotification insertNotification(String deviceId, String key, DeviceNotification deviceNotification) {
        Map<String, String> headers = getHeaders(deviceId, key);
        HiveValidator.validate(deviceNotification);
        String path = "/device/" + deviceId + "notification";
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, headers, null, deviceNotification,
                DeviceNotification.class, NOTIFICATION_FROM_DEVICE, NOTIFICATION_TO_DEVICE);

    }

    private Map<String, String> getHeaders(String deviceId, String key) {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.DEVICE_ID_HEADER, deviceId);
        headers.put(Constants.DEVICE_KEY_HEADER, key);
        return headers;
    }

    public ApiInfo getInfo() {
        return hiveContext.getInfo();
    }

}
