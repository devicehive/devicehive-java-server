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

    @Override
    public void authenticate(String login, String password) throws HiveException {
        restAgent.authenticate(HivePrincipal.createUser(login, password));
    }

    @Override
    public void authenticate(String accessKey) throws HiveException {
        restAgent.authenticate(HivePrincipal.createAccessKey(accessKey));
    }

    @Override
    public AccessKeyController getAccessKeyController() {
        return new AccessKeyControllerImpl(restAgent);
    }

    @Override
    public CommandsController getCommandsController() {
        return new CommandsControllerRestImpl(restAgent);
    }

    @Override
    public DeviceController getDeviceController() {
        return new DeviceControllerImpl(restAgent);
    }

    @Override
    public NetworkController getNetworkController() {
        return new NetworkControllerImpl(restAgent);
    }

    @Override
    public NotificationsController getNotificationsController() {
        return new NotificationsControllerRestImpl(restAgent);
    }

    @Override
    public UserController getUserController() {
        return new UserControllerImpl(restAgent);
    }

    @Override
    public OAuthClientController getOAuthClientController() {
        return new OAuthClientControllerImpl(restAgent);
    }

    @Override
    public OAuthGrantController getOAuthGrantController() {
        return new OAuthGrantControllerImpl(restAgent);
    }

    @Override
    public OAuthTokenController getOAuthTokenController() {
        return new OAuthTokenControllerImpl(restAgent);
    }

    @Override
    public void close() {
        restAgent.close();
    }


}
