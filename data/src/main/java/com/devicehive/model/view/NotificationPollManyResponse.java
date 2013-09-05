package com.devicehive.model.view;


import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.annotations.SerializedName;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT;

public class NotificationPollManyResponse implements HiveEntity {

    private static final long serialVersionUID = -4390548037685312874L;
    @SerializedName("notification")
    @JsonPolicyDef(NOTIFICATION_TO_CLIENT)
    private DeviceNotificationView notification;

    @SerializedName("deviceGuid")
    @JsonPolicyDef(NOTIFICATION_TO_CLIENT)
    private String guid;

    public NotificationPollManyResponse(DeviceNotificationView notification, String guid) {
        this.notification = notification;
        this.guid = guid;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public DeviceNotificationView getNotification() {
        return notification;
    }

    public void setNotification(DeviceNotificationView notification) {
        this.notification = notification;
    }
}
