package com.devicehive.model.rpc;

import com.devicehive.model.DeviceNotification;
import com.devicehive.shim.api.Body;

import java.util.Collections;
import java.util.List;

public class NotificationSearchResponse extends Body {

    private List<DeviceNotification> notifications;

    public NotificationSearchResponse() {
        super(Action.NOTIFICATION_SEARCH_RESPONSE.name());
        this.notifications = Collections.emptyList();
    }

    public List<DeviceNotification> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<DeviceNotification> notifications) {
        this.notifications = notifications;
    }
}
