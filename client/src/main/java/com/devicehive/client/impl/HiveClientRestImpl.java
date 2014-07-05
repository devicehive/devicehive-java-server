package com.devicehive.client.impl;


import com.devicehive.client.*;
import com.devicehive.client.impl.context.HivePrincipal;
import com.devicehive.client.impl.context.RestAgent;
import com.devicehive.client.model.ApiInfo;
import com.devicehive.client.model.exceptions.HiveException;

public class HiveClientRestImpl implements HiveClient {

    private final RestAgent restAgent;

    public HiveClientRestImpl(RestAgent restAgent) {
        this.restAgent = restAgent;
    }

    @Override
    public ApiInfo getInfo() throws HiveException {
        return restAgent.getInfo();
    }

    public void authenticate(String login, String password) throws HiveException {
        restAgent.authenticate(HivePrincipal.createUser(login, password));
    }

    public void authenticate(String accessKey) throws HiveException {
        restAgent.authenticate(HivePrincipal.createAccessKey(accessKey));
    }

    public AccessKeyController getAccessKeyController() {
        return new AccessKeyControllerImpl(restAgent);
    }

    public CommandsController getCommandsController() {
        return new CommandsControllerRestImpl(restAgent);
    }

    public DeviceController getDeviceController() {
        return new DeviceControllerImpl(restAgent);
    }

    public NetworkController getNetworkController() {
        return new NetworkControllerImpl(restAgent);
    }

    public NotificationsController getNotificationsController() {
        return new NotificationsControllerRestImpl(restAgent);
    }

    public UserController getUserController() {
        return new UserControllerImpl(restAgent);
    }

    public OAuthClientController getOAuthClientController() {
        return new OAuthClientControllerImpl(restAgent);
    }

    public OAuthGrantController getOAuthGrantController() {
        return new OAuthGrantControllerImpl(restAgent);
    }

    public OAuthTokenController getOAuthTokenController() {
        return new OAuthTokenControllerImpl(restAgent);
    }

    public void close() throws HiveException {
        restAgent.close();
    }


}
