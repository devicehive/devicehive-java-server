package com.devicehive.websockets;

import com.devicehive.json.strategies.JsonPolicyDef;
import org.apache.commons.lang3.ObjectUtils;
import java.util.Date;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_TO_DEVICE;

public class InsertNotification {

    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    private Long id;

    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    private Date timestamp;

    public InsertNotification(Long id, Date timestamp) {
        this.id = id;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public Date getTimestamp() {
        return ObjectUtils.cloneIfPossible(timestamp);
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = ObjectUtils.cloneIfPossible(timestamp);
    }

}
