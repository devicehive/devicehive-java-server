package com.devicehive.client.context;

import com.devicehive.client.model.DeviceNotification;

public interface NotificationHandler {

    void handle(DeviceNotification notification);

}
