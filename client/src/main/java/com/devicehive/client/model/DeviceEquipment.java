package com.devicehive.client.model;

import com.devicehive.client.impl.json.strategies.JsonPolicyDef;

import org.apache.commons.lang3.ObjectUtils;

import java.sql.Timestamp;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.DEVICE_EQUIPMENT_SUBMITTED;

/**
 * Device Equipment
 */
public class DeviceEquipment implements HiveEntity {

    private static final long serialVersionUID = 479737367629574073L;

    @JsonPolicyDef(DEVICE_EQUIPMENT_SUBMITTED)
    private Long id;

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
        return ObjectUtils.cloneIfPossible(timestamp);
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = ObjectUtils.cloneIfPossible(timestamp);
    }

    public JsonStringWrapper getParameters() {
        return parameters;
    }

    public void setParameters(JsonStringWrapper parameters) {
        this.parameters = parameters;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DeviceEquipment{");
        sb.append("id=").append(id);
        sb.append(", code='").append(code).append('\'');
        sb.append(", timestamp=").append(timestamp);
        sb.append(", parameters=").append(parameters);
        sb.append('}');
        return sb.toString();
    }
}
