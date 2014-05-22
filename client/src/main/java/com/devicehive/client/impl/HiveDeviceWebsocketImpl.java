package com.devicehive.client.impl;


import com.devicehive.client.MessageHandler;
import com.devicehive.client.impl.context.HiveWebsocketContext;
import com.devicehive.client.impl.json.GsonFactory;
import com.devicehive.client.model.Device;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.SubscriptionFilter;
import com.devicehive.client.model.exceptions.HiveException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.tuple.Pair;

import javax.ws.rs.HttpMethod;
import java.sql.Timestamp;
import java.util.UUID;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.COMMAND_UPDATE_FROM_DEVICE;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.DEVICE_PUBLISHED_DEVICE_AUTH;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_FROM_DEVICE;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_TO_DEVICE;


public class HiveDeviceWebsocketImpl extends HiveDeviceRestImpl {

    private HiveWebsocketContext hiveContext;

    public HiveDeviceWebsocketImpl(HiveWebsocketContext hiveContext) {
        super(hiveContext);
        this.hiveContext = hiveContext;
    }

    @Override
    public Device getDevice() throws HiveException {
        JsonObject request = new JsonObject();
        request.addProperty("action", "device/get");
        return hiveContext.getWebsocketConnector().sendMessage(request, "device", Device.class,
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
        hiveContext.getWebsocketConnector().sendMessage(request);
    }

    @Override
    public DeviceCommand getCommand(long commandId) throws HiveException {
        Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
        String path = "/device/" + authenticated.getKey() + "/command/" + commandId;
        return hiveContext.getRestConnector().execute(path, HttpMethod.GET, null, DeviceCommand.class, null);
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
        hiveContext.getWebsocketConnector().sendMessage(request);
    }

    @Override
    public void subscribeForCommands(final Timestamp timestamp, MessageHandler<DeviceCommand> commandMessageHandler)
            throws
            HiveException {
        SubscriptionFilter filter =
                new SubscriptionFilter(hiveContext.getHivePrincipal().getDevice().getLeft(), null, timestamp);
        hiveContext.addCommandsSubscription(filter, commandMessageHandler);

    }

    /**
     * Unsubscribes the device from commands.
     */
    @Override
    public void unsubscribeFromCommands(String subId) throws HiveException {
        hiveContext.removeCommandsSubscription(subId);
    }

    @Override
    public DeviceNotification insertNotification(DeviceNotification deviceNotification) throws HiveException {
        JsonObject request = new JsonObject();
        request.addProperty("action", "notification/insert");
        Gson gson = GsonFactory.createGson(NOTIFICATION_FROM_DEVICE);
        request.add("notification", gson.toJsonTree(deviceNotification));
        return hiveContext.getWebsocketConnector().sendMessage(request, "notification", DeviceNotification.class,
                NOTIFICATION_TO_DEVICE);
    }


}
