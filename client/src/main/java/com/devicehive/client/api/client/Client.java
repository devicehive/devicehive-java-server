package com.devicehive.client.api.client;


import com.devicehive.client.context.HiveContext;
import com.devicehive.client.context.HivePrincipal;
import com.devicehive.client.model.ApiInfo;
import com.devicehive.client.model.Transport;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

public class Client implements HiveClient {

    private static final String CLIENT_ENDPOINT_PATH = "/client";
    private final HiveContext hiveContext;

    public Client(URI uri, URI websocket) {
        hiveContext = new HiveContext(Transport.AUTO, uri, URI.create(websocket.toString() + CLIENT_ENDPOINT_PATH));
    }

    public Client(URI uri, URI websocket, Transport transport) {
        hiveContext = new HiveContext(transport, uri, URI.create(websocket.toString() + CLIENT_ENDPOINT_PATH));
    }

    public ApiInfo getInfo() {
        return hiveContext.getInfo();
    }

    public void authenticate(String login, String password) {
        if (hiveContext.useSockets()) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "authenticate");
            String requestId = UUID.randomUUID().toString();
            request.addProperty("requestId", requestId);
            request.addProperty("login", login);
            request.addProperty("password", password);
            hiveContext.getHiveWebSocketClient().sendMessage(request);
        }
        hiveContext.setHivePrincipal(HivePrincipal.createUser(login, password));
    }

    public void authenticate(String accessKey) {
        if (hiveContext.useSockets()) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "authenticate");
            String requestId = UUID.randomUUID().toString();
            request.addProperty("requestId", requestId);
            request.addProperty("accessKey", accessKey);
            hiveContext.getHiveWebSocketClient().sendMessage(request);
        }
        hiveContext.setHivePrincipal(HivePrincipal.createAccessKey(accessKey));

    }

    public AccessKeyController getAccessKeyController() {
        return new AccessKeyControllerImpl(hiveContext);
    }

    public CommandsController getCommandsController() {
        return new CommandsControllerImpl(hiveContext);
    }

    public DeviceController getDeviceController() {
        return new DeviceControllerImpl(hiveContext);
    }

    public NetworkController getNetworkController() {
        return new NetworkControllerImpl(hiveContext);
    }

    public NotificationsController getNotificationsController() {
        return new NotificationsControllerImpl(hiveContext);
    }

    public UserController getUserController() {
        return new UserControllerImpl(hiveContext);
    }

    public OAuthClientController getOAuthClientController() {
        return new OAuthClientControllerImpl(hiveContext);
    }

    public OAuthGrantController getOAuthGrantController() {
        return new OAuthGrantControllerImpl(hiveContext);
    }

    public OAuthTokenController getOAuthTokenController() {
        return new OAuthTokenControllerImpl(hiveContext);
    }

    @Override
    public void close() throws IOException {
        hiveContext.close();
    }

}
