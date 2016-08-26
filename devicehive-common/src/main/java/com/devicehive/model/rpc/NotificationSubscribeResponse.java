package com.devicehive.model.rpc;

import com.devicehive.model.DeviceNotification;
import com.devicehive.shim.api.Body;

import java.util.Collection;
import java.util.Objects;

import static com.devicehive.configuration.Constants.*;

public class NotificationSubscribeResponse extends Body {
    private String subId;
    private Collection<DeviceNotification> notifications;

    public NotificationSubscribeResponse(String subId, Collection<DeviceNotification> notifications) {
        super(Action.NOTIFICATION_SUBSCRIBE_RESPONSE.name());
        this.subId = subId;
        this.notifications = notifications;
    }

    public String getSubId() {
        return subId;
    }

    public void setSubId(String subId) {
        this.subId = subId;
    }

    public Collection<DeviceNotification> getNotifications() {
        return notifications;
    }

    public void setNotifications(Collection<DeviceNotification> notifications) {
        this.notifications = notifications;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotificationSubscribeResponse)) return false;
        if (!super.equals(o)) return false;
        NotificationSubscribeResponse that = (NotificationSubscribeResponse) o;
        return Objects.equals(subId, that.subId) &&
                Objects.equals(notifications, that.notifications);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), subId, notifications);
    }

    @Override
    public String toString() {
        return "NotificationSubscribeResponse{" +
                "subId='" + subId + '\'' +
                ", notifications=" + notifications +
                '}';
    }
}
