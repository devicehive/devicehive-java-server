package com.devicehive.client.api.client;


import com.devicehive.client.api.AuthenticationService;
import com.devicehive.client.context.HiveContext;
import com.devicehive.client.context.HivePrincipal;
import com.devicehive.client.model.ApiInfo;
import com.devicehive.client.model.Role;
import com.devicehive.client.model.Transport;

import java.io.IOException;
import java.net.URI;

public class Client implements HiveClient {


    private final HiveContext hiveContext;

    public Client(URI uri) {
        hiveContext = new HiveContext(Transport.AUTO, uri, Role.USER);
    }

    public Client(URI uri, Transport transport) {
        hiveContext = new HiveContext(transport, uri, Role.USER);
    }

    public ApiInfo getInfo() {
        return hiveContext.getInfo();
    }

    public void authenticate(String login, String password) {
        if (hiveContext.useSockets()) {
            AuthenticationService.authenticateClient(login, password, hiveContext);
        }
        hiveContext.setHivePrincipal(HivePrincipal.createUser(login, password));
    }

    public void authenticate(String accessKey) {
        if (hiveContext.useSockets()) {
            AuthenticationService.authenticateKey(accessKey, hiveContext);
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
