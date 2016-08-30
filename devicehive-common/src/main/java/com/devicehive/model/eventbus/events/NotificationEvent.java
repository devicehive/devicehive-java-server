package com.devicehive.model.eventbus.events;

import com.devicehive.model.DeviceNotification;
import com.devicehive.model.eventbus.Subscription;
import com.devicehive.model.rpc.Action;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

public class NotificationEvent extends Event {

    private DeviceNotification notification;

    public NotificationEvent(DeviceNotification notification) {
        super(Action.NOTIFICATION_EVENT.name());
        this.notification = notification;
    }

    public DeviceNotification getNotification() {
        return notification;
    }

    public void setNotification(DeviceNotification notification) {
        this.notification = notification;
    }

    @Override
    public Collection<Subscription> getApplicableSubscriptions() {
        Subscription deviceOnly =
                new Subscription(Action.NOTIFICATION_EVENT.name(), notification.getDeviceGuid());
        Subscription deviceWithName =
                new Subscription(Action.NOTIFICATION_EVENT.name(), notification.getDeviceGuid(), notification.getNotification());
        return Arrays.asList(deviceOnly, deviceWithName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotificationEvent)) return false;
        if (!super.equals(o)) return false;
        NotificationEvent that = (NotificationEvent) o;
        return Objects.equals(notification, that.notification);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), notification);
    }

    @Override
    public String toString() {
        return "NotificationEvent{" +
                "notification=" + notification +
                '}';
    }
}
