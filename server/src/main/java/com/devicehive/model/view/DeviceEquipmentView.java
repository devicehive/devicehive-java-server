package com.devicehive.model.view;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.NullableWrapper;
import com.devicehive.model.domain.DeviceEquipment;

import java.sql.Timestamp;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_EQUIPMENT_SUBMITTED;

public class DeviceEquipmentView implements HiveEntity {

    private static final long serialVersionUID = 479737367629574073L;

    @JsonPolicyDef(DEVICE_EQUIPMENT_SUBMITTED)
    private NullableWrapper<String> code;

    @JsonPolicyDef(DEVICE_EQUIPMENT_SUBMITTED)
    private NullableWrapper<Timestamp> timestamp;

    @JsonPolicyDef(DEVICE_EQUIPMENT_SUBMITTED)
    private NullableWrapper<JsonStringWrapper> parameters;

    public NullableWrapper<String> getCode() {
        return code;
    }

    public void setCode(NullableWrapper<String> code) {
        this.code = code;
    }

    public NullableWrapper<Timestamp> getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(NullableWrapper<Timestamp> timestamp) {
        this.timestamp = timestamp;
    }

    public NullableWrapper<JsonStringWrapper> getParameters() {
        return parameters;
    }

    public void setParameters(NullableWrapper<JsonStringWrapper> parameters) {
        this.parameters = parameters;
    }

    public DeviceEquipment convertTo() {
        DeviceEquipment deviceEquipment = new DeviceEquipment();
        if (code != null) {
            deviceEquipment.setCode(code.getValue());
        }
        if (timestamp != null) {
            deviceEquipment.setTimestamp(timestamp.getValue());
        }
        if (parameters != null) {
            deviceEquipment.setParameters(parameters.getValue());
        }
        return deviceEquipment;
    }

    public void convertFrom(DeviceEquipment deviceEquipment) {
        if (deviceEquipment == null){
            return;
        }
        code = new NullableWrapper<>(deviceEquipment.getCode());
        timestamp = new NullableWrapper<>(deviceEquipment.getTimestamp());
        parameters = new NullableWrapper<>(deviceEquipment.getParameters());
    }
}
