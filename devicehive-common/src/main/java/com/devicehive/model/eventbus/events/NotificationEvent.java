package com.devicehive.model.eventbus.events;

/*
 * #%L
 * DeviceHive Common Module
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
                new Subscription(Action.NOTIFICATION_EVENT.name(), notification.getDeviceId());
        Subscription deviceWithName =
                new Subscription(Action.NOTIFICATION_EVENT.name(), notification.getDeviceId(), notification.getNotification());
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
