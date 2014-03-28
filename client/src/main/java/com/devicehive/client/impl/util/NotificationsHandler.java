package com.devicehive.client.impl.util;

import com.devicehive.client.model.DeviceNotification;

public interface NotificationsHandler {

    boolean handle(DeviceNotification notification);
}
