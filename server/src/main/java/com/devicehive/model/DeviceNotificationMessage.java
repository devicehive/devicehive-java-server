package com.devicehive.model;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.ObjectUtils;

import java.sql.Timestamp;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_FROM_DEVICE;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_TO_DEVICE;

/**
 * Created by tmatvienko on 12/24/14.
 */
public class DeviceNotificationMessage implements HiveEntity {
    private static final long serialVersionUID = 1834383778016225837L;

    @SerializedName("parameters")
    @JsonPolicyDef({NOTIFICATION_FROM_DEVICE, NOTIFICATION_TO_CLIENT})
    private JsonObject parameters;

    @SerializedName("timestamp")
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    private Timestamp timestamp;

    @SerializedName("notification")
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    private String notification;

    @SerializedName("device_guid")
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    private String deviceGuid;

    public DeviceNotificationMessage() {
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

    public JsonObject getParameters() {
        return parameters;
    }

    public void setParameters(JsonObject parameters) {
        this.parameters = parameters;
    }

    public String getDeviceGuid() {
        return deviceGuid;
    }

    public void setDeviceGuid(String deviceGuid) {
        this.deviceGuid = deviceGuid;
    }

    @Override
    public String toString() {
        return "DeviceNotificationMessage{" +
                "parameters=" + parameters +
                ", timestamp=" + timestamp +
                ", notification='" + notification + '\'' +
                ", deviceGuid='" + deviceGuid + '\'' +
                '}';
    }
}
