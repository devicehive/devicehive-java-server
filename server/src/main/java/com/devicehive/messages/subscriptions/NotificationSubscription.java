package com.devicehive.messages.subscriptions;


import com.devicehive.auth.HivePrincipal;
import com.devicehive.messages.handler.HandlerCreator;
import com.devicehive.model.DeviceNotification;

import java.util.UUID;

public class NotificationSubscription extends Subscription<String, DeviceNotification> {

    private final HivePrincipal principal;

    private final String notificationNames;

    public NotificationSubscription(HivePrincipal principal, String deviceGuid, UUID subscriberId,
                                    String notificationNames,
                                    HandlerCreator<DeviceNotification> handlerCreator) {
        super(deviceGuid, subscriberId, handlerCreator);
        this.principal = principal;
        this.notificationNames = notificationNames != null ? notificationNames : null;
    }

    public String getDeviceGuid() {
        return getEventSource();
    }


    public HivePrincipal getPrincipal() {
        return principal;
    }

    public String getNotificationNames() {
        return notificationNames;
    }


}
