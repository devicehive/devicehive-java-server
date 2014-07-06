package com.devicehive.messages.subscriptions;


import com.devicehive.auth.HivePrincipal;
import com.devicehive.messages.handler.HandlerCreator;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class NotificationSubscription extends Subscription<Long, DeviceNotification> {

    private final HivePrincipal principal;

    private final Set<String> notificationNames;

    public NotificationSubscription(HivePrincipal principal, Long deviceId, UUID subscriberId,
                                    Collection<String> notificationNames,
                                    HandlerCreator<DeviceNotification>  handlerCreator) {
        super(deviceId, subscriberId, handlerCreator);
        this.principal = principal;
        this.notificationNames = notificationNames != null ? new HashSet<>(notificationNames) : null;
    }

    public Long getDeviceId() {
        return getEventSource();
    }


    public HivePrincipal getPrincipal() {
        return principal;
    }

    public Set<String> getNotificationNames() {
        return notificationNames;
    }


}
