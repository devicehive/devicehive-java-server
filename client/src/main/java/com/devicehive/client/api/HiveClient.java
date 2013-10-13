package com.devicehive.client.api;


import com.devicehive.client.model.ApiInfo;

public interface HiveClient {

    ApiInfo getInfo();

    void authenticate(String login, String password);

    AccessKeyController getAccessKeyController();

    CommandsController getCommandsController();

    DeviceController getDeviceController();

    NetworkContorller getNetworkContorller();

    NotificationsController getNotificationsController();

    UserContorller getUserContorller();

}
