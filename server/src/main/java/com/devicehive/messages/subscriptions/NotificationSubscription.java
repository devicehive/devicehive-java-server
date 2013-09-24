package com.devicehive.messages.subscriptions;


import com.devicehive.auth.HivePrincipal;
import com.devicehive.messages.handler.HandlerCreator;

public class NotificationSubscription extends Subscription<Long> {

    private final HivePrincipal principal;

    public NotificationSubscription(HivePrincipal principal, Long deviceId, String subscriberId,
                                    HandlerCreator handlerCreator) {
        super(deviceId, subscriberId, handlerCreator);
        this.principal = principal;
    }

    public Long getDeviceId() {
        return getEventSource();
    }

    public String getSessionId() {
        return getSubscriberId();
    }

    public HivePrincipal getPrincipal(){
        return principal;
    }
}
