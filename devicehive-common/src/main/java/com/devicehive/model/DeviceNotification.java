package com.devicehive.model;

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

import com.devicehive.configuration.Constants;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serial;
import java.util.Date;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_FROM_DEVICE;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_TO_DEVICE;

public class DeviceNotification implements HiveEntity, CacheEntity {

    @Serial
    private static final long serialVersionUID = 1834383778016225837L;

    @SerializedName("id")
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    private Long id;

    @SerializedName("notification")
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    private String notification;

    @SerializedName("deviceId")
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    private String deviceId;

    @SerializedName("networkId")
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    private Long networkId;

    @SerializedName("deviceTypeId")
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    private Long deviceTypeId;

    @SerializedName("timestamp")
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    @SerializedName("parameters")
    @JsonPolicyDef({NOTIFICATION_FROM_DEVICE, NOTIFICATION_TO_CLIENT})
    private JsonStringWrapper parameters;

    public Long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getTimestamp() {
        return ObjectUtils.cloneIfPossible(timestamp);
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = ObjectUtils.cloneIfPossible(timestamp);
    }

    public String getNotification() {
        return notification;
    }

    public void setNotification(String notification) {
        this.notification = notification;
    }

    public JsonStringWrapper getParameters() {
        return parameters;
    }

    public void setParameters(JsonStringWrapper parameters) {
        this.parameters = parameters;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public void setNetworkId(Long networkId) {
        this.networkId = networkId;
    }

    public Long getDeviceTypeId() {
        return deviceTypeId;
    }

    public void setDeviceTypeId(Long deviceTypeId) {
        this.deviceTypeId = deviceTypeId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        final DeviceNotification that = (DeviceNotification) o;

        return new EqualsBuilder().append(getId(), that.getId())
                                  .append(getNotification(), that.getNotification())
                                  .append(getDeviceId(), that.getDeviceId())
                                  .append(getNetworkId(), that.getNetworkId())
                                  .append(getDeviceTypeId(), that.getDeviceTypeId())
                                  .append(getTimestamp(), that.getTimestamp())
                                  .append(getParameters(), that.getParameters())
                                  .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(getId())
                                          .append(getNotification())
                                          .append(getDeviceId())
                                          .append(getNetworkId())
                                          .append(getDeviceTypeId())
                                          .append(getTimestamp())
                                          .append(getParameters())
                                          .toHashCode();
    }

    @Override
    public String toString() {
        return "DeviceNotification{" +
                "id=" + id +
                ", notification='" + notification + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", networkId='" + networkId + '\'' +
                ", deviceTypeId='" + deviceTypeId + '\'' +
                ", timestamp=" + timestamp +
                ", parameters='" + parameters + '\'' +
                '}';
    }

    @JsonIgnore
    @Override
    public String getCacheKey() {
        return String.format("%s_%s_%s", Constants.NOTIFICATIONS, deviceId, id);
    }
}
