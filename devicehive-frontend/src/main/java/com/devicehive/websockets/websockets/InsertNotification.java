package com.devicehive.websockets.websockets;

import com.devicehive.json.strategies.JsonPolicyDef;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Date;

public class InsertNotification {
    @JsonPolicyDef({JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT, JsonPolicyDef.Policy.NOTIFICATION_TO_DEVICE})
    private Long id;
    @JsonPolicyDef({JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT, JsonPolicyDef.Policy.NOTIFICATION_TO_DEVICE})
    private Date timestamp;

    public InsertNotification(Long id, Date timestamp) {
        this.id = id;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return this.id;
    }

    public Date getTimestamp() {
        return ObjectUtils.cloneIfPossible(this.timestamp);
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = (Date)ObjectUtils.cloneIfPossible(timestamp);
    }
}
