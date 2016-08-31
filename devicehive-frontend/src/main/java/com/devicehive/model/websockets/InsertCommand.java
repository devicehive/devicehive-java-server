package com.devicehive.model.websockets;

import com.devicehive.json.strategies.JsonPolicyDef;

import java.util.Date;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class InsertCommand {

    @JsonPolicyDef({COMMAND_TO_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, POST_COMMAND_TO_DEVICE,
            COMMAND_LISTED})
    private Long id;

    @JsonPolicyDef({COMMAND_TO_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private Date timestamp;

    @JsonPolicyDef({COMMAND_TO_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_LISTED})
    private Long userId;

    public InsertCommand(Long id, Date timestamp, Long userId) {
        this.id = id;
        this.timestamp = timestamp;
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public Long getUserId() {
        return userId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
