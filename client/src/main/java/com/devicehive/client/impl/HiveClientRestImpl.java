package com.devicehive.client.impl;


import com.devicehive.client.AccessKeyController;
import com.devicehive.client.CommandsController;
import com.devicehive.client.DeviceController;
import com.devicehive.client.HiveClient;
import com.devicehive.client.NetworkController;
import com.devicehive.client.NotificationsController;
import com.devicehive.client.OAuthClientController;
import com.devicehive.client.OAuthGrantController;
import com.devicehive.client.OAuthTokenController;
import com.devicehive.client.UserController;
import com.devicehive.client.impl.context.HivePrincipal;
import com.devicehive.client.impl.context.HiveRestContext;
import com.devicehive.client.model.ApiInfo;
import com.devicehive.client.model.exceptions.HiveException;

public class HiveClientRestImpl implements HiveClient {


    private final HiveRestContext hiveContext;


    public HiveClientRestImpl(HiveRestContext hiveContext) {
        this.hiveContext = hiveContext;
    }

    public ApiInfo getInfo() throws HiveException {
        return hiveContext.getInfo();
    }

    public void authenticate(String login, String password) throws HiveException {
        hiveContext.authenticate(HivePrincipal.createUser(login, password));
    }

    public void authenticate(String accessKey) throws HiveException {
        hiveContext.authenticate(HivePrincipal.createAccessKey(accessKey));
    }

    public AccessKeyController getAccessKeyController() {
        return new AccessKeyControllerImpl(hiveContext);
    }

    public CommandsController getCommandsController() {
        return new CommandsControllerRestImpl(hiveContext);
    }

    public DeviceController getDeviceController() {
        return new DeviceControllerImpl(hiveContext);
    }

    public NetworkController getNetworkController() {
        return new NetworkControllerImpl(hiveContext);
    }

    public NotificationsController getNotificationsController() {
        return new NotificationsControllerRestImpl(hiveContext);
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

    public void close() {
        hiveContext.close();
    }

}
