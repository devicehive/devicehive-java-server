package com.devicehive.model.view;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.domain.DeviceNotification;

import java.sql.Timestamp;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class DeviceNotificationView implements HiveEntity {

    private static final long serialVersionUID = 8704321978956225955L;
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    private Long id;
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    private Timestamp timestamp;
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_FROM_DEVICE})
    private String notification;
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_FROM_DEVICE})
    private JsonStringWrapper parameters;
    @JsonPolicyDef(NOTIFICATION_TO_DEVICE)
    private DeviceSubscriptionView device;

    public DeviceNotificationView() {
    }

    public DeviceNotificationView(DeviceNotification deviceNotification) {
        convertFrom(deviceNotification);
    }

    public DeviceSubscriptionView getDevice() {
        return device;
    }

    public void setDevice(DeviceSubscriptionView device) {
        this.device = device;
    }

    public JsonStringWrapper getParameters() {
        return parameters;
    }

    public void setParameters(JsonStringWrapper parameters) {
        this.parameters = parameters;
    }

    public String getNotification() {
        return notification;
    }

    public void setNotification(String notification) {
        this.notification = notification;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DeviceNotification convertTo() {
        DeviceNotification deviceNotification = new DeviceNotification();
        deviceNotification.setId(id);
        deviceNotification.setTimestamp(timestamp);
        deviceNotification.setParameters(parameters);
        deviceNotification.setNotification(notification);
        return deviceNotification;
    }

    public void convertFrom(DeviceNotification deviceNotification) {
        if (deviceNotification == null) {
            return;
        }
        id = deviceNotification.getId();
        timestamp = deviceNotification.getTimestamp();
        parameters = deviceNotification.getParameters();
        notification = deviceNotification.getNotification();
        device = new DeviceSubscriptionView(deviceNotification.getDevice().getGuid(),
                deviceNotification.getDevice().getId());
    }
}
