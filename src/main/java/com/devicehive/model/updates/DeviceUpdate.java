package com.devicehive.model.updates;


import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.*;
import com.google.gson.annotations.SerializedName;

import java.util.UUID;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_PUBLISHED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_SUBMITTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NETWORK_PUBLISHED;

public class DeviceUpdate implements HiveEntity {
    @SerializedName("id")
    @JsonPolicyDef({DEVICE_PUBLISHED, NETWORK_PUBLISHED})
    NullableWrapper<UUID> guid;
    @SerializedName("key")
    @JsonPolicyDef({DEVICE_SUBMITTED})
    NullableWrapper<String> key;
    @SerializedName("name")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    NullableWrapper<String> name;
    @SerializedName("status")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    NullableWrapper<String> status;
    @SerializedName("data")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    NullableWrapper<JsonStringWrapper> data;
    @SerializedName("network")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED})
    NullableWrapper<Network> network;
    @SerializedName("deviceClass")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    NullableWrapper<DeviceClassUpdate> deviceClass;

    public NullableWrapper<DeviceClassUpdate> getDeviceClass() {
        return deviceClass;
    }

    public void setDeviceClass(NullableWrapper<DeviceClassUpdate> deviceClass) {
        this.deviceClass = deviceClass;
    }

    public NullableWrapper<UUID> getGuid() {
        return guid;
    }

    public void setGuid(NullableWrapper<UUID> guid) {
        this.guid = guid;
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

    public NullableWrapper<Network> getNetwork() {
        return network;
    }

    public void setNetwork(NullableWrapper<Network> network) {
        this.network = network;
    }

    public Device convertTo() {
        Device device = new Device();
        if (guid != null)
            device.setGuid(guid.getValue());
        if (data != null) {
            device.setData(data.getValue());
        }
        if (deviceClass != null) {
            DeviceClass deviceClassToConvert = new DeviceClass();
            DeviceClassUpdate deviceClassUpdate = deviceClass.getValue();
            if (deviceClassUpdate.getVersion() != null) {
                deviceClassToConvert.setVersion(deviceClassUpdate.getVersion().getValue());
            }
            if (deviceClassUpdate.getData() != null) {
                deviceClassToConvert.setData(deviceClassUpdate.getData().getValue());
            }
            if (deviceClassUpdate.getOfflineTimeout() != null) {
                deviceClassToConvert.setOfflineTimeout(deviceClassUpdate.getOfflineTimeout().getValue());
            }
            if (deviceClassUpdate.getEquipment() != null) {
                deviceClassToConvert.setEquipment(deviceClassUpdate.getEquipment().getValue());
            }
            if (deviceClassUpdate.getPermanent() != null) {
                deviceClassToConvert.setPermanent(deviceClassUpdate.getPermanent().getValue());
            }
            if (deviceClassUpdate.getName() != null) {
                deviceClassToConvert.setName(deviceClassUpdate.getName().getValue());
            }
            device.setDeviceClass(deviceClassToConvert);
        }
        if (key != null) {
            device.setKey(key.getValue());
        }
        if (name != null) {
            device.setName(name.getValue());
        }
        if (network != null) {
            device.setNetwork(network.getValue());
        }
        if (status != null) {
            device.setStatus(status.getValue());
        }
        return device;
    }

}
