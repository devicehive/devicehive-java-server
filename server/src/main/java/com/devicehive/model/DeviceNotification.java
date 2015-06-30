package com.devicehive.model;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.ObjectUtils;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class DeviceNotification implements HiveEntity, HazelcastEntity {
    private static final long serialVersionUID = 1834383778016225837L;

    @SerializedName("id")
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    private Long id;

    @SerializedName("notification")
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    private String notification;

    @SerializedName("deviceGuid")
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    private String deviceGuid;

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

        DeviceNotification message = (DeviceNotification) o;

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
        return "DeviceNotification{" +
                "id=" + id +
                ", notification='" + notification + '\'' +
                ", deviceGuid='" + deviceGuid + '\'' +
                ", timestamp=" + timestamp +
                ", parameters='" + parameters + '\'' +
                '}';
    }

    @Override
    public String getHazelcastKey() {
        return id+"-"+deviceGuid+"-"+timestamp;
    }
}
