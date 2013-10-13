package com.devicehive.client.api;


import com.devicehive.client.context.CommandHandler;
import com.devicehive.client.context.CommandUpdateHandler;
import com.devicehive.client.context.NotificationHandler;
import com.devicehive.client.model.*;

import java.sql.Timestamp;
import java.util.List;

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
