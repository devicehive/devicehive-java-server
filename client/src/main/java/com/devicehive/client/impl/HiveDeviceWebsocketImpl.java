package com.devicehive.client.impl;


import com.devicehive.client.HiveDevice;
import com.devicehive.client.impl.context.HiveContext;
import com.devicehive.client.impl.context.HivePrincipal;
import com.devicehive.client.impl.json.GsonFactory;
import com.devicehive.client.impl.util.HiveValidator;
import com.devicehive.client.impl.websocket.WebsocketAuthenticationUtil;
import com.devicehive.client.impl.websocket.WebsocketSubscriptionsUtil;
import com.devicehive.client.model.*;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.tuple.Pair;

import javax.ws.rs.HttpMethod;
import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.util.*;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.*;


public class HiveDeviceWebsocketImpl extends HiveDeviceRestImpl {

    private HiveContext hiveContext;

    /**
     * Creates new device, that can communicate with the server via provided URLs.
     *
     * @param restUri      RESTful service URL
     */
    HiveDeviceWebsocketImpl(URI restUri) {
        super(restUri);
    }

    /**
     * Creates new simple device, that can communicate with the server via provided URLs.
     *
     * @param restUri      RESTful service URL
     * @param transport    transport to use
     */
    public HiveDeviceWebsocketImpl(URI restUri, Transport transport) {
        super(restUri, transport);
    }



    @Override
    public void authenticate(String deviceId, String deviceKey) {
        super.authenticate(deviceId, deviceKey);
        if (hiveContext.useSockets()) {
            WebsocketAuthenticationUtil.authenticateDevice(deviceId, deviceKey, hiveContext);
        }
    }


    @Override
    public Device getDevice() {
            JsonObject request = new JsonObject();
            request.addProperty("action", "device/get");
            String requestId = UUID.randomUUID().toString();
            request.addProperty("requestId", requestId);
            return hiveContext.getHiveWebSocketClient().sendMessage(request, "device", Device.class,
                    DEVICE_PUBLISHED_DEVICE_AUTH);
    }


    @Override
    public void registerDevice(Device device) {
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
    }




    @Override
    public DeviceCommand getCommand(long commandId) {
        Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
        String path = "/device/" + authenticated.getKey() + "/command/" + commandId;
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, DeviceCommand.class, null);
    }


    @Override
    public void updateCommand(DeviceCommand deviceCommand) {
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
    public void subscribeForCommands(final Timestamp timestamp) {
            WebsocketSubscriptionsUtil.subscribeDeviceForCommands(hiveContext, timestamp);

    }

    /**
     * Unsubscribes the device from commands.
     */
    @Override
    public void unsubscribeFromCommands() {
            JsonObject request = new JsonObject();
            request.addProperty("action", "command/unsubscribe");
            String requestId = UUID.randomUUID().toString();
            request.addProperty("requestId", requestId);
            hiveContext.getHiveWebSocketClient().sendMessage(request);
            Pair<String, String> authenticated = hiveContext.getHivePrincipal().getDevice();
            hiveContext.getHiveSubscriptions().removeWsCommandSubscription(null, authenticated.getLeft());
    }


    @Override
    public DeviceNotification insertNotification(DeviceNotification deviceNotification) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "notification/insert");
            String requestId = UUID.randomUUID().toString();
            request.addProperty("requestId", requestId);
            Gson gson = GsonFactory.createGson(NOTIFICATION_FROM_DEVICE);
            request.add("notification", gson.toJsonTree(deviceNotification));
            return hiveContext.getHiveWebSocketClient().sendMessage(request, "notification", DeviceNotification.class,
                    NOTIFICATION_TO_DEVICE);
    }


}
