package com.devicehive.model.response;


import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.DeviceNotificationMessage;
import com.devicehive.model.HiveEntity;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT;

public class NotificationPollManyResponse implements HiveEntity {

    private static final long serialVersionUID = -4390548037685312874L;
    @SerializedName("notification")
    @JsonPolicyDef(NOTIFICATION_TO_CLIENT)
    private DeviceNotificationMessage notification;

    @SerializedName("deviceGuid")
    @JsonPolicyDef(NOTIFICATION_TO_CLIENT)
    private String guid;

    public NotificationPollManyResponse(DeviceNotificationMessage notification, String guid) {
        this.notification = notification;
        this.guid = guid;
    }

    public static List<NotificationPollManyResponse> getList(List<DeviceNotificationMessage> notifications) {
        List<NotificationPollManyResponse> result = new ArrayList<>(notifications.size());
        for (DeviceNotificationMessage notification : notifications) {
            result.add(new NotificationPollManyResponse(notification, notification.getDeviceGuid()));
        }
        return result;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public DeviceNotificationMessage getNotification() {
        return notification;
    }

    public void setNotification(DeviceNotificationMessage notification) {
        this.notification = notification;
    }
}
