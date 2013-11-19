package com.devicehive.client.api.client;


import com.devicehive.client.model.ApiInfo;

import java.io.Closeable;

public interface HiveClient extends Closeable {

    ApiInfo getInfo();

    void authenticate(String login, String password);

    AccessKeyController getAccessKeyController();

    CommandsController getCommandsController();

    DeviceController getDeviceController();

    NetworkController getNetworkController();

    NotificationsController getNotificationsController();

    UserController getUserController();

}
