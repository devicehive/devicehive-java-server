package com.devicehive.client.impl;


import com.devicehive.client.impl.context.HiveContext;
import com.devicehive.client.impl.json.GsonFactory;
import com.devicehive.client.model.*;
import com.devicehive.client.model.exceptions.HiveException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.tuple.Pair;

import javax.ws.rs.HttpMethod;
import java.sql.Timestamp;
import java.util.*;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.*;


public class HiveDeviceWebsocketImpl extends HiveDeviceRestImpl {


    public HiveDeviceWebsocketImpl(HiveContext hiveContext) {
        super(hiveContext);
    }

    @Override
    public void authenticate(String deviceId, String deviceKey) throws HiveException {
        super.authenticate(deviceId, deviceKey);
        JsonObject request = new JsonObject();
        request.addProperty("action", "authenticate");
        request.addProperty("deviceId", deviceId);
        request.addProperty("deviceKey", deviceKey);
        hiveContext.getHiveWebSocketClient().sendMessage(request);
    }


    @Override
    public Device getDevice() throws HiveException {
        JsonObject request = new JsonObject();
        request.addProperty("action", "device/get");
        return hiveContext.getHiveWebSocketClient().sendMessage(request, "device", Device.class,
                DEVICE_PUBLISHED_DEVICE_AUTH);
    }


    @Override
    public void registerDevice(Device device) throws HiveException {
        JsonObject request = new JsonObject();
        request.addProperty("action", "device/save");
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
    }


    @Override
    public DeviceCommand getCommand(long commandId) throws HiveException {
        Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
        String path = "/device/" + authenticated.getKey() + "/command/" + commandId;
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, DeviceCommand.class, null);
    }


    @Override
    public void updateCommand(DeviceCommand deviceCommand) throws HiveException {
        JsonObject request = new JsonObject();
        request.addProperty("action", "command/update");
        String requestId = UUID.randomUUID().toString();
        request.addProperty("requestId", requestId);
        request.addProperty("commandId", deviceCommand.getId());
        Gson gson = GsonFactory.createGson(COMMAND_UPDATE_FROM_DEVICE);
        request.add("command", gson.toJsonTree(deviceCommand));
        hiveContext.getHiveWebSocketClient().sendMessage(request);
    }


    @Override
    public void subscribeForCommands(final Timestamp timestamp) throws HiveException {
        hiveContext.getWebsocketSubManager().addCommandsSubscription(timestamp, null,
                hiveContext.getHivePrincipal().getDevice().getLeft());

    }

    /**
     * Unsubscribes the device from commands.
     */
    @Override
    public void unsubscribeFromCommands() throws HiveException {
        JsonObject request = new JsonObject();
        request.addProperty("action", "command/unsubscribe");
        hiveContext.getHiveWebSocketClient().sendMessage(request);
        Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
        hiveContext.getWebsocketSubManager().removeCommandSubscription(null, authenticated.getLeft());
    }


    @Override
    public DeviceNotification insertNotification(DeviceNotification deviceNotification) throws HiveException {
        JsonObject request = new JsonObject();
        request.addProperty("action", "notification/insert");
        Gson gson = GsonFactory.createGson(NOTIFICATION_FROM_DEVICE);
        request.add("notification", gson.toJsonTree(deviceNotification));
        return hiveContext.getHiveWebSocketClient().sendMessage(request, "notification", DeviceNotification.class,
                NOTIFICATION_TO_DEVICE);
    }


}
