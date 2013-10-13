package com.devicehive.client.model;

import com.devicehive.client.json.strategies.JsonPolicyDef;
import org.apache.commons.lang3.ObjectUtils;

import java.sql.Timestamp;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.*;

public class DeviceNotification implements HiveEntity {

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

    public DeviceNotification() {
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
        return ObjectUtils.cloneIfPossible(timestamp);
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = ObjectUtils.cloneIfPossible(timestamp);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


}
