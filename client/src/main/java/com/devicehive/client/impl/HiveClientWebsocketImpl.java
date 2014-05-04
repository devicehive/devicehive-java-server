package com.devicehive.client.impl;


import com.devicehive.client.CommandsController;
import com.devicehive.client.NotificationsController;
import com.devicehive.client.impl.context.HivePrincipal;
import com.devicehive.client.impl.context.WebsocketHiveContext;
import com.devicehive.client.model.exceptions.HiveException;

public class HiveClientWebsocketImpl extends HiveClientRestImpl {

    private final WebsocketHiveContext hiveContext;

    public HiveClientWebsocketImpl(WebsocketHiveContext hiveContext) {
        super(hiveContext);
        this.hiveContext = hiveContext;
    }

    public void authenticate(String login, String password) throws HiveException {
        hiveContext.authenticate(HivePrincipal.createUser(login, password));
    }

    public void authenticate(String accessKey) throws HiveException {
        hiveContext.authenticate(HivePrincipal.createAccessKey(accessKey));
    }


    public CommandsController getCommandsController() {
        return new CommandsControllerWebsocketImpl(hiveContext);
    }


    public NotificationsController getNotificationsController() {
        return new NotificationsControllerWebsocketImpl(hiveContext);
    }


}
