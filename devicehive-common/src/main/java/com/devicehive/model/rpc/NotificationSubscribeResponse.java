package com.devicehive.model.rpc;

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
import com.devicehive.shim.api.Action;
import com.devicehive.shim.api.Body;

import java.util.Collection;
import java.util.Objects;

public class NotificationSubscribeResponse extends Body {
    private Long subId;
    private Collection<DeviceNotification> notifications;

    public NotificationSubscribeResponse(Long subId, Collection<DeviceNotification> notifications) {
        super(Action.NOTIFICATION_SUBSCRIBE_RESPONSE);
        this.subId = subId;
        this.notifications = notifications;
    }

    public Long getSubId() {
        return subId;
    }

    public void setSubId(Long subId) {
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
