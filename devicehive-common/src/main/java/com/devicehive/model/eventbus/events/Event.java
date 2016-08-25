package com.devicehive.model.eventbus.events;

import com.devicehive.model.eventbus.Subscription;
import com.devicehive.shim.api.Body;

import java.util.Collection;

public abstract class Event extends Body {

    public Event(String action) {
        super(action);
    }

    /**
     * Returns applicable to this event subscriptions.
     * For example, if event is device_notification { deviceGuid = a, notificationName = b },
     * then subscriptions on { guid = a, name = null } and { guid = a, name = b } will be returned,
     * but not { guid = a, name = c }, cause it is not applicable to this event.
     *
     * @return collection of potentially valid subscriptions, for which this event can be routed.
     */
    public abstract Collection<Subscription> getApplicableSubscriptions();

}
