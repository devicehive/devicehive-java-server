package com.devicehive.client.model;

import com.devicehive.client.json.strategies.JsonPolicyDef;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.*;

public class DeviceSubscription implements HiveEntity {

    private static final long serialVersionUID = 2959997451631843333L;

    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT})
    private Long id;

    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT})
    private String guid;

    public DeviceSubscription() {
    }

    public DeviceSubscription(String guid, Long id) {
        this.id = id;
        this.guid = guid;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }
}
