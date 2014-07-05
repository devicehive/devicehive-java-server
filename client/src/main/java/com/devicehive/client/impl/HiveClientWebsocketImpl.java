package com.devicehive.client.impl;


import com.devicehive.client.CommandsController;
import com.devicehive.client.NotificationsController;
import com.devicehive.client.impl.context.HivePrincipal;
import com.devicehive.client.impl.context.WebsocketAgent;
import com.devicehive.client.model.exceptions.HiveException;

public class HiveClientWebsocketImpl extends HiveClientRestImpl {

    private final WebsocketAgent websocketAgent;

    public HiveClientWebsocketImpl(WebsocketAgent websocketAgent) {
        super(websocketAgent);
        this.websocketAgent = websocketAgent;
    }

    public void authenticate(String login, String password) throws HiveException {
        websocketAgent.authenticate(HivePrincipal.createUser(login, password));
    }

    public void authenticate(String accessKey) throws HiveException {
        websocketAgent.authenticate(HivePrincipal.createAccessKey(accessKey));
    }


    public CommandsController getCommandsController() {
        return new CommandsControllerWebsocketImpl(websocketAgent);
    }


    public NotificationsController getNotificationsController() {
        return new NotificationsControllerWebsocketImpl(websocketAgent);
    }


}
