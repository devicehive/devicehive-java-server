package com.devicehive.model.wrappers;

/*
 * #%L
 * DeviceHive Java Server Common business logic
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

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.JsonStringWrapper;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

/**
 * Created by tmatvienko on 12/24/14.
 */
public class DeviceNotificationWrapper implements HiveEntity {
    private static final long serialVersionUID = 2377186341017341138L;

    @SerializedName("notification")
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE, NOTIFICATION_FROM_DEVICE})
    private String notification;

    @SerializedName("timestamp")
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE, NOTIFICATION_FROM_DEVICE})
    private Date timestamp;

    @SerializedName("parameters")
    @JsonPolicyDef({NOTIFICATION_FROM_DEVICE, NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    private JsonStringWrapper parameters;

    public String getNotification() {
        return notification;
    }

    public void setNotification(String notification) {
        this.notification = notification;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public JsonStringWrapper getParameters() {
        return parameters;
    }

    public void setParameters(JsonStringWrapper parameters) {
        this.parameters = parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeviceNotificationWrapper message = (DeviceNotificationWrapper) o;

        if (notification != null ? !notification.equals(message.notification) : message.notification != null)
            return false;
        if (parameters != null ? !parameters.equals(message.parameters) : message.parameters != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = notification != null ? notification.hashCode() : 0;
        result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DeviceNotificationWrapper{" +
                "notification='" + notification + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}
