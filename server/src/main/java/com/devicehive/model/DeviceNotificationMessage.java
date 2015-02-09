package com.devicehive.model;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.ObjectUtils;

import java.sql.Timestamp;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

/**
 * Created by tmatvienko on 12/24/14.
 */
public class DeviceNotificationMessage implements HiveEntity {
    private static final long serialVersionUID = 1834383778016225837L;

    @SerializedName("id")
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    private String id;

    @SerializedName("notification")
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    private String notification;

    @SerializedName("device_guid")
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    private String deviceGuid;

    @SerializedName("timestamp")
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    private Timestamp timestamp;

    @SerializedName("parameters")
    @JsonPolicyDef({NOTIFICATION_FROM_DEVICE, NOTIFICATION_TO_CLIENT})
    private String parameters;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Timestamp getTimestamp() {
        return ObjectUtils.cloneIfPossible(timestamp);
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = ObjectUtils.cloneIfPossible(timestamp);
    }

    public String getNotification() {
        return notification;
    }

    public void setNotification(String notification) {
        this.notification = notification;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public String getDeviceGuid() {
        return deviceGuid;
    }

    public void setDeviceGuid(String deviceGuid) {
        this.deviceGuid = deviceGuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeviceNotificationMessage message = (DeviceNotificationMessage) o;

        if (deviceGuid != null ? !deviceGuid.equals(message.deviceGuid) : message.deviceGuid != null) return false;
        if (id != null ? !id.equals(message.id) : message.id != null) return false;
        if (notification != null ? !notification.equals(message.notification) : message.notification != null)
            return false;
        if (parameters != null ? !parameters.equals(message.parameters) : message.parameters != null) return false;
        if (timestamp != null ? !timestamp.equals(message.timestamp) : message.timestamp != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (notification != null ? notification.hashCode() : 0);
        result = 31 * result + (deviceGuid != null ? deviceGuid.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DeviceNotificationMessage{" +
                "id=" + id +
                ", notification='" + notification + '\'' +
                ", deviceGuid='" + deviceGuid + '\'' +
                ", timestamp=" + timestamp +
                ", parameters='" + parameters + '\'' +
                '}';
    }
}
