package com.devicehive.model.view;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.NullableWrapper;
import com.devicehive.model.domain.Device;
import com.devicehive.model.domain.DeviceClass;

import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class DeviceView implements HiveEntity {

    private static final long serialVersionUID = -7498444232044147881L;
    @JsonPolicyDef({DEVICE_PUBLISHED, NETWORK_PUBLISHED})
    private String id;
    @JsonPolicyDef({DEVICE_SUBMITTED, DEVICE_PUBLISHED})
    private NullableWrapper<String> key;
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private NullableWrapper<String> name;
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private NullableWrapper<String> status;
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private NullableWrapper<JsonStringWrapper> data;
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED})
    private NullableWrapper<NetworkView> network;
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private NullableWrapper<DeviceClassView> deviceClass;
    @JsonPolicyDef(DEVICE_PUBLISHED)
    private NullableWrapper<Set<EquipmentView>> equipment;

    public DeviceView() {
    }

    public DeviceView(Device device){
        convertFrom(device);
    }

    public NullableWrapper<Set<EquipmentView>> getEquipment() {
        return equipment;
    }

    public void setEquipment(NullableWrapper<Set<EquipmentView>> equipment) {
        this.equipment = equipment;
    }

    public NullableWrapper<DeviceClassView> getDeviceClass() {
        return deviceClass;
    }

    public void setDeviceClass(NullableWrapper<DeviceClassView> deviceClass) {
        this.deviceClass = deviceClass;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public NullableWrapper<String> getKey() {
        return key;
    }

    public void setKey(NullableWrapper<String> key) {
        this.key = key;
    }

    public NullableWrapper<String> getName() {
        return name;
    }

    public void setName(NullableWrapper<String> name) {
        this.name = name;
    }

    public NullableWrapper<String> getStatus() {
        return status;
    }

    public void setStatus(NullableWrapper<String> status) {
        this.status = status;
    }

    public NullableWrapper<JsonStringWrapper> getData() {
        return data;
    }

    public void setData(NullableWrapper<JsonStringWrapper> data) {
        this.data = data;
    }

    public NullableWrapper<NetworkView> getNetwork() {
        return network;
    }

    public void setNetwork(NullableWrapper<NetworkView> network) {
        this.network = network;
    }

    public Device convertTo() {
        Device device = new Device();
        device.setGuid(id);
        if (data != null) {
            device.setData(data.getValue());
        }
        if (deviceClass != null) {
            DeviceClass convertedDeviceClass = deviceClass.getValue().convertTo();
            device.setDeviceClass(convertedDeviceClass);
        }
        if (key != null) {
            device.setKey(key.getValue());
        }
        if (name != null) {
            device.setName(name.getValue());
        }
        if (network != null) {
            device.setNetwork(network.getValue().convertTo());
        }
        if (status != null) {
            device.setStatus(status.getValue());
        }
        return device;
    }

    public void convertFrom(Device device) {
        if (device == null){
            return;
        }
        id = device.getGuid();
        data = new NullableWrapper<>(device.getData());
        DeviceClassView deviceClassView = new DeviceClassView();
        deviceClassView.convertFrom(device.getDeviceClass());
        deviceClass = new NullableWrapper<>(deviceClassView);
        key = new NullableWrapper<>(device.getKey());
        name = new NullableWrapper<>(device.getName());
        NetworkView networkView = new NetworkView();
        networkView.convertFrom(device.getNetwork());
        network = new NullableWrapper<>(networkView);
        status = new NullableWrapper<>(device.getStatus());
    }
}
