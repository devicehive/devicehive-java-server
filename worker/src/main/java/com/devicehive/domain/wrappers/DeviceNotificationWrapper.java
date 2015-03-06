package com.devicehive.domain.wrappers;

import com.devicehive.domain.JsonStringWrapper;
import com.devicehive.messages.converter.adapter.TimestampAdapter;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Created by tmatvienko on 2/24/15.
 */
public class DeviceNotificationWrapper implements Serializable {

    @SerializedName("id")
    private Long id;

    @SerializedName("deviceGuid")
    private String deviceGuid;

    @SerializedName("timestamp")
    private Timestamp timestamp;

    @SerializedName("notification")
    private String notification;

    @SerializedName("parameters")
    private JsonStringWrapper parameters;

    public DeviceNotificationWrapper() {
    }

    public DeviceNotificationWrapper(Long id, String deviceGuid, Timestamp timestamp, String notification, JsonStringWrapper parameters) {
        this.id = id;
        this.deviceGuid = deviceGuid;
        this.timestamp = timestamp;
        this.notification = notification;
        this.parameters= parameters;
    }

    public Long getId() {
        return id;
    }

    public String getNotification() {
        return notification;
    }

    public String getDeviceGuid() {
        return deviceGuid;
    }

    @JsonSerialize(using=TimestampAdapter.class)
    public Timestamp getTimestamp() {
        return timestamp;
    }

    public JsonStringWrapper getParameters() {
        return parameters;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setDeviceGuid(String deviceGuid) {
        this.deviceGuid = deviceGuid;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public void setNotification(String notification) {
        this.notification = notification;
    }

    public void setParameters(JsonStringWrapper parameters) {
        this.parameters = parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeviceNotificationWrapper that = (DeviceNotificationWrapper) o;

        if (deviceGuid != null ? !deviceGuid.equals(that.deviceGuid) : that.deviceGuid != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (notification != null ? !notification.equals(that.notification) : that.notification != null) return false;
        if (parameters != null ? !parameters.equals(that.parameters) : that.parameters != null) return false;
        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (deviceGuid != null ? deviceGuid.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (notification != null ? notification.hashCode() : 0);
        result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DeviceNotificationWrapper{" +
                "id=" + id +
                ", notification='" + notification + '\'' +
                ", deviceGuid=" + deviceGuid +
                ", timestamp=" + timestamp +
                '}';
    }
}
