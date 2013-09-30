package com.devicehive.client.model;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.JsonStringWrapper;

import java.sql.Timestamp;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_EQUIPMENT_SUBMITTED;

public class DeviceEquipment implements HiveEntity {

    private static final long serialVersionUID = 479737367629574073L;

    @JsonPolicyDef(DEVICE_EQUIPMENT_SUBMITTED)
    private String code;

    @JsonPolicyDef(DEVICE_EQUIPMENT_SUBMITTED)
    private Timestamp timestamp;

    @JsonPolicyDef(DEVICE_EQUIPMENT_SUBMITTED)
    private JsonStringWrapper parameters;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public JsonStringWrapper getParameters() {
        return parameters;
    }

    public void setParameters(JsonStringWrapper parameters) {
        this.parameters = parameters;
    }
}
