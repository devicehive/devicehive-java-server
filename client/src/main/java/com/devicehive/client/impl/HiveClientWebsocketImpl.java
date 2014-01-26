package com.devicehive.client.impl;


import com.devicehive.client.*;
import com.devicehive.client.impl.context.HiveContext;
import com.devicehive.client.model.exceptions.HiveException;
import com.google.gson.JsonObject;

public class HiveClientWebsocketImpl extends HiveClientRestImpl {

    public HiveClientWebsocketImpl(HiveContext hiveContext) {
        super(hiveContext);
    }

    public void authenticate(String login, String password) throws HiveException {
        super.authenticate(login, password);
        JsonObject request = new JsonObject();
        request.addProperty("action", "authenticate");
        request.addProperty("login", login);
        request.addProperty("password", password);
        hiveContext.getHiveWebSocketClient().sendMessage(request);
    }

    public void authenticate(String accessKey) throws HiveException {
        super.authenticate(accessKey);
        JsonObject request = new JsonObject();
        request.addProperty("action", "authenticate");
        request.addProperty("accessKey", accessKey);
        hiveContext.getHiveWebSocketClient().sendMessage(request);
    }


    public CommandsController getCommandsController() {
        return new CommandsControllerWebsocketImpl(hiveContext);
    }


    public NotificationsController getNotificationsController() {
        return new NotificationsControllerWebsocketImpl(hiveContext);
    }


}
