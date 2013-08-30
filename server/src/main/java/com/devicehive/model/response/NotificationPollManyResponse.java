package com.devicehive.model.response;


import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.HiveEntity;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT;

public class NotificationPollManyResponse implements HiveEntity {

    private static final long serialVersionUID = -4390548037685312874L;
    @SerializedName("notification")
    @JsonPolicyDef(NOTIFICATION_TO_CLIENT)
    private DeviceNotification notification;

    @SerializedName("deviceGuid")
    @JsonPolicyDef(NOTIFICATION_TO_CLIENT)
    private String guid;

    public NotificationPollManyResponse(DeviceNotification notification, String guid) {
        this.notification = notification;
        this.guid = guid;
    }

    public static List<NotificationPollManyResponse> getList(List<DeviceNotification> notifications) {
        List<NotificationPollManyResponse> result = new ArrayList<>(notifications.size());
        for (DeviceNotification notification : notifications) {
            result.add(new NotificationPollManyResponse(notification, notification.getDevice().getGuid()));
        }
        return result;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public DeviceNotification getNotification() {
        return notification;
    }

    public void setNotification(DeviceNotification notification) {
        this.notification = notification;
    }
}
