package com.devicehive.messages.subscriptions;


import com.devicehive.messages.handler.HandlerCreator;
import com.devicehive.model.domain.User;

public class NotificationSubscription extends Subscription<Long> {

    private final User user;

    public NotificationSubscription(User user, Long deviceId, String subscriberId, HandlerCreator handlerCreator) {
        super(deviceId, subscriberId, handlerCreator);
        this.user = user;
    }

    public Long getDeviceId() {
        return getEventSource();
    }

    public String getSessionId() {
        return getSubscriberId();
    }

    public User getUser() {
        return user;
    }
}
