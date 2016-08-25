package com.devicehive.model.rpc;

import com.devicehive.model.DeviceNotification;
import com.devicehive.shim.api.Body;

public class NotificationInsertRequest extends Body {

    private DeviceNotification deviceNotification;

    public NotificationInsertRequest(DeviceNotification deviceNotification) {
        super(Action.NOTIFICATION_INSERT.name());
        this.deviceNotification = deviceNotification;
    }

    public DeviceNotification getDeviceNotification() {
        return deviceNotification;
    }

    public void setDeviceNotification(DeviceNotification deviceNotification) {
        this.deviceNotification = deviceNotification;
    }
}
