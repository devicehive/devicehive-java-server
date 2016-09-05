package com.devicehive.model.rpc;

import com.devicehive.model.DeviceNotification;
import com.devicehive.shim.api.Body;

import java.util.List;

public class NotificationSearchResponse extends Body {

    private List<DeviceNotification> notifications;

    public NotificationSearchResponse(List<DeviceNotification> notifications) {
        super(Action.NOTIFICATION_SEARCH_RESPONSE.name());
        this.notifications = notifications;
    }

    public List<DeviceNotification> getNotifications() {
        return notifications;
    }
}
