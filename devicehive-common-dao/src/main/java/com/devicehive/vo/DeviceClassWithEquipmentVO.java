package com.devicehive.vo;

import com.devicehive.json.strategies.JsonPolicyDef;

import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICECLASS_PUBLISHED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_PUBLISHED;

/**
 *
 */
public class DeviceClassWithEquipmentVO extends DeviceClassVO {

    @JsonPolicyDef({DEVICECLASS_PUBLISHED, DEVICE_PUBLISHED})
    private Set<DeviceClassEquipmentVO> equipment;

    public Set<DeviceClassEquipmentVO> getEquipment() {
        return equipment;
    }

    public void setEquipment(Set<DeviceClassEquipmentVO> equipment) {
        this.equipment = equipment;
    }
}
