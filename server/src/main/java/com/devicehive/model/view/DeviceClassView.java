package com.devicehive.model.view;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.NullableWrapper;
import com.devicehive.model.domain.DeviceClass;
import com.devicehive.model.domain.Equipment;

import java.util.HashSet;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class DeviceClassView implements HiveEntity {
    private static final long serialVersionUID = 967472386318199376L;
    @JsonPolicyDef(
            {DEVICE_PUBLISHED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED, DEVICECLASS_SUBMITTED})
    Long id;
    @JsonPolicyDef({DEVICECLASS_PUBLISHED})
    NullableWrapper<Set<EquipmentView>> equipment;
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

    public DeviceClassView() {
    }

    public DeviceClassView(DeviceClass deviceClass) {
        convertFrom(deviceClass);
    }

    public NullableWrapper<Set<EquipmentView>> getEquipment() {
        return equipment;
    }

    public void setEquipment(NullableWrapper<Set<EquipmentView>> equipment) {
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
        if (equipment != null && equipment.getValue() != null) {
            Set<Equipment> result = new HashSet<>(equipment.getValue().size());
            for (EquipmentView current : equipment.getValue()) {
                result.add(current.convertTo());
            }
            deviceClass.setEquipment(result);
        }
        return deviceClass;
    }

    public void convertFrom(DeviceClass deviceClass) {
        if (deviceClass == null) {
            return;
        }
        id = deviceClass.getId();
        data = new NullableWrapper<>(deviceClass.getData());
        name = new NullableWrapper<>(deviceClass.getName());
        version = new NullableWrapper<>(deviceClass.getVersion());
        isPermanent = new NullableWrapper<>(deviceClass.getPermanent());
        offlineTimeout = new NullableWrapper<>(deviceClass.getOfflineTimeout());
        if (deviceClass.getEquipment() != null) {
            Set<EquipmentView> res = new HashSet<>(deviceClass.getEquipment().size());
            for (Equipment current : deviceClass.getEquipment()) {
                res.add(new EquipmentView(current));
            }
            equipment = new NullableWrapper<>(res);
        }
    }
}
