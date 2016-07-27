package com.devicehive.model.updates;


import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.Equipment;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.vo.DeviceClassEquipmentVO;
import com.devicehive.vo.DeviceClassWithEquipmentVO;

import java.util.Optional;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class DeviceClassUpdate implements HiveEntity {

    private static final long serialVersionUID = 967472386318199376L;
    @JsonPolicyDef(DEVICE_PUBLISHED)
    private Long id;
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED})
    private Optional<String> name;
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED})
    private Optional<Boolean> isPermanent;
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED})
    private Optional<Integer> offlineTimeout;
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED})
    private Optional<JsonStringWrapper> data;
    @JsonPolicyDef({DEVICECLASS_PUBLISHED, DEVICE_SUBMITTED})
    private Optional<Set<DeviceClassEquipmentVO>> equipment;

    public Optional<Set<DeviceClassEquipmentVO>> getEquipment() {
        return equipment;
    }

    public void setEquipment(Optional<Set<DeviceClassEquipmentVO>> equipment) {
        this.equipment = equipment;
    }

    public Optional<String> getName() {
        return name;
    }

    public void setName(Optional<String> name) {
        this.name = name;
    }

    public Optional<Boolean> getPermanent() {
        return isPermanent;
    }

    public void setPermanent(Optional<Boolean> permanent) {
        isPermanent = permanent;
    }

    public Optional<Integer> getOfflineTimeout() {
        return offlineTimeout;
    }

    public void setOfflineTimeout(Optional<Integer> offlineTimeout) {
        this.offlineTimeout = offlineTimeout;
    }

    public Optional<JsonStringWrapper> getData() {
        return data;
    }

    public void setData(Optional<JsonStringWrapper> data) {
        this.data = data;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DeviceClassWithEquipmentVO convertTo() {
        DeviceClassWithEquipmentVO deviceClass = new DeviceClassWithEquipmentVO();
        deviceClass.setId(id);
        if (isPermanent != null) {
            deviceClass.setIsPermanent(isPermanent.orElse(null));
        }
        if (offlineTimeout != null) {
            deviceClass.setOfflineTimeout(offlineTimeout.orElse(null));
        }
        if (data != null) {
            deviceClass.setData(data.orElse(null));
        }
        if (name != null) {
            deviceClass.setName(name.orElse(null));
        }
        if (equipment != null) {
            deviceClass.setEquipment(equipment.orElse(null));
        }
        return deviceClass;
    }
}
