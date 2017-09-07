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

import com.devicehive.model.eventbus.Filter;
import com.devicehive.shim.api.Action;
import com.devicehive.shim.api.Body;

import java.util.Date;
import java.util.Objects;
import java.util.Set;

public class NotificationSubscribeRequest extends Body {

    private Long subscriptionId;
    private String device;
    private Filter filter;
    private Date timestamp;

    public NotificationSubscribeRequest(Long subscriptionId, String device, Filter filter, Date timestamp) {
        super(Action.NOTIFICATION_SUBSCRIBE_REQUEST);
        this.subscriptionId = subscriptionId;
        this.device = device;
        this.filter = filter;
        this.timestamp = timestamp;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotificationSubscribeRequest)) return false;
        if (!super.equals(o)) return false;
        NotificationSubscribeRequest that = (NotificationSubscribeRequest) o;
        return Objects.equals(subscriptionId, that.subscriptionId) &&
                Objects.equals(device, that.device) &&
                Objects.equals(filter, that.filter) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), subscriptionId, device, filter, timestamp);
    }

    @Override
    public String toString() {
        return "NotificationSubscribeRequest{" +
                "subscriptionId='" + subscriptionId + '\'' +
                ", device='" + device + '\'' +
                ", filter=" + filter +
                ", timestamp=" + timestamp +
                '}';
    }
}
