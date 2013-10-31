package com.devicehive.client.api;


import com.devicehive.client.model.ApiInfo;

import java.io.Closeable;

public interface HiveClient extends Closeable {

    ApiInfo getInfo();

    void authenticate(String login, String password);

    AccessKeyController getAccessKeyController();

    CommandsController getCommandsController();

    DeviceController getDeviceController();

    NetworkContorller getNetworkController();

    NotificationsController getNotificationsController();

    UserContorller getUserController();

}
