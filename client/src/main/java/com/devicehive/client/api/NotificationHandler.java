package com.devicehive.client.api;

import com.devicehive.client.model.DeviceNotification;

public interface NotificationHandler {

    void handle(DeviceNotification notification);

}
