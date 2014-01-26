package com.devicehive.client.impl;


import com.devicehive.client.*;
import com.devicehive.client.impl.context.HiveContext;
import com.devicehive.client.impl.context.HivePrincipal;
import com.devicehive.client.model.ApiInfo;
import com.devicehive.client.model.exceptions.HiveException;

import java.io.IOException;

public class HiveClientRestImpl implements HiveClient {


    protected final HiveContext hiveContext;


    public HiveClientRestImpl(HiveContext hiveContext) {
        this.hiveContext = hiveContext;
    }

    public ApiInfo getInfo() throws HiveException {
        return hiveContext.getInfo();
    }

    public void authenticate(String login, String password) throws HiveException {
        hiveContext.setHivePrincipal(HivePrincipal.createUser(login, password));
    }

    public void authenticate(String accessKey) throws HiveException {
        hiveContext.setHivePrincipal(HivePrincipal.createAccessKey(accessKey));
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

    @Override
    public void close() throws IOException {
        hiveContext.close();
    }

}
