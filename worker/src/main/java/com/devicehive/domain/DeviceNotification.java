package com.devicehive.domain;

import com.devicehive.domain.wrappers.DeviceNotificationWrapper;
import org.springframework.cassandra.core.PrimaryKeyType;
import org.springframework.data.cassandra.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.mapping.Table;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by tmatvienko on 2/5/15.
 */
@Table(value = "device_notification")
public class DeviceNotification implements Serializable {

    @PrimaryKeyColumn(name = "id", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private String id;

    @PrimaryKeyColumn(name = "device_guid", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private String deviceGuid;

    private Date timestamp;

    private String notification;

    private String parameters;

    public DeviceNotification() {
    }

    public DeviceNotification(String id, String deviceGuid, Date timestamp, String notification, String parameters) {
        this.id = id;
        this.deviceGuid = deviceGuid;
        this.timestamp = timestamp;
        this.notification = notification;
        this.parameters= parameters;
    }

    public DeviceNotification(DeviceNotificationWrapper wrapper) {
        if (wrapper.getId() != null) {
            this.id = wrapper.getId().toString();
        }
        if (wrapper.getDeviceGuid() != null) {
            this.deviceGuid = wrapper.getDeviceGuid();
        }
        if (wrapper.getTimestamp() != null) {
            this.timestamp = wrapper.getTimestamp();
        }
        if (wrapper.getNotification() != null) {
            this.notification = wrapper.getNotification();
        }
        if (wrapper.getParameters() != null) {
            this.parameters = wrapper.getParameters().getJsonString();
        }
    }

    public String getId() {
        return id;
    }

    public String getNotification() {
        return notification;
    }

    public String getDeviceGuid() {
        return deviceGuid;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getParameters() {
        return parameters;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDeviceGuid(String deviceGuid) {
        this.deviceGuid = deviceGuid;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void setNotification(String notification) {
        this.notification = notification;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeviceNotification that = (DeviceNotification) o;

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
        return "DeviceNotification{" +
                "id=" + id +
                ", notification='" + notification + '\'' +
                ", deviceGuid=" + deviceGuid +
                ", timestamp=" + timestamp +
                '}';
    }
}
