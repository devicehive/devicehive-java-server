package com.devicehive.model.rpc;

import com.devicehive.model.DeviceNotification;
import com.devicehive.shim.api.Body;

public class NotificationInsertResponse extends Body {

    private DeviceNotification deviceNotification;

    public NotificationInsertResponse(DeviceNotification deviceNotification) {
        super(Action.NOTIFICATION_INSERT_RESPONSE.name());
        this.deviceNotification = deviceNotification;
    }

    public DeviceNotification getDeviceNotification() {
        return deviceNotification;
    }

    public void setDeviceNotification(DeviceNotification deviceNotification) {
        this.deviceNotification = deviceNotification;
    }
}
