package com.devicehive.model.updates;


import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.*;

import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class DeviceClassUpdate implements HiveEntity {
    private static final long serialVersionUID = 967472386318199376L;
    @JsonPolicyDef(DEVICE_PUBLISHED)
    private Long id;
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED})
    private NullableWrapper<String> name;
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED})
    private NullableWrapper<String> version;
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED})
    private NullableWrapper<Boolean> isPermanent;
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED})
    private NullableWrapper<Integer> offlineTimeout;
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED})
    private NullableWrapper<JsonStringWrapper> data;
    @JsonPolicyDef({DEVICECLASS_PUBLISHED})
    private NullableWrapper<Set<Equipment>> equipment;

    public NullableWrapper<Set<Equipment>> getEquipment() {
        return equipment;
    }

    public void setEquipment(NullableWrapper<Set<Equipment>> equipment) {
        this.equipment = equipment;
    }

    public NullableWrapper<String> getName() {
        return name;
    }

    public void setName(NullableWrapper<String> name) {
        this.name = name;
    }

    public NullableWrapper<String> getVersion() {
        return version;
    }

    public void setVersion(NullableWrapper<String> version) {
        this.version = version;
    }

    public NullableWrapper<Boolean> getPermanent() {
        return isPermanent;
    }

    public void setPermanent(NullableWrapper<Boolean> permanent) {
        isPermanent = permanent;
    }

    public NullableWrapper<Integer> getOfflineTimeout() {
        return offlineTimeout;
    }

    public void setOfflineTimeout(NullableWrapper<Integer> offlineTimeout) {
        this.offlineTimeout = offlineTimeout;
    }

    public NullableWrapper<JsonStringWrapper> getData() {
        return data;
    }

    public void setData(NullableWrapper<JsonStringWrapper> data) {
        this.data = data;
    }

    public DeviceClass convertTo() {
        DeviceClass deviceClass = new DeviceClass();
        deviceClass.setId(id);
        if (isPermanent != null) {
            deviceClass.setPermanent(isPermanent.getValue());
        }
        if (offlineTimeout != null) {
            deviceClass.setOfflineTimeout(offlineTimeout.getValue());
        }
        if (data != null) {
            deviceClass.setData(data.getValue());
        }
        if (name != null) {
            deviceClass.setName(name.getValue());
        }
        if (version != null) {
            deviceClass.setVersion(version.getValue());
        }
        if (equipment != null) {
            deviceClass.setEquipment(equipment.getValue());
        }
        return deviceClass;
    }
}
