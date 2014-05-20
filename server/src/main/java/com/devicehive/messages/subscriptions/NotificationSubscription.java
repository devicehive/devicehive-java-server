package com.devicehive.messages.subscriptions;


import com.devicehive.auth.HivePrincipal;
import com.devicehive.messages.handler.HandlerCreator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class NotificationSubscription extends Subscription<Long> {

    private final HivePrincipal principal;

    private final Set<String> notificationNames;

    public NotificationSubscription(HivePrincipal principal, Long deviceId, UUID subscriberId,
                                    Collection<String> notificationNames,
                                    HandlerCreator handlerCreator) {
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
