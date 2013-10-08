package com.devicehive.model.updates;


import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.*;
import com.google.gson.annotations.SerializedName;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class DeviceUpdate implements HiveEntity {

    private static final long serialVersionUID = -7498444232044147881L;
    @SerializedName("id")
    @JsonPolicyDef({DEVICE_PUBLISHED, NETWORK_PUBLISHED})
    private NullableWrapper<String> guid;

    @SerializedName("key")
    @JsonPolicyDef({DEVICE_SUBMITTED, DEVICE_PUBLISHED})
    private NullableWrapper<String> key;

    @SerializedName("name")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private NullableWrapper<String> name;

    @SerializedName("status")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private NullableWrapper<String> status;

    @SerializedName("data")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private NullableWrapper<JsonStringWrapper> data;

    @SerializedName("network")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED})
    private NullableWrapper<Network> network;

    @SerializedName("deviceClass")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private NullableWrapper<DeviceClassUpdate> deviceClass;

    public NullableWrapper<DeviceClassUpdate> getDeviceClass() {
        return deviceClass;
    }

    public void setDeviceClass(NullableWrapper<DeviceClassUpdate> deviceClass) {
        this.deviceClass = deviceClass;
    }

    public NullableWrapper<String> getGuid() {
        return guid;
    }

    public void setGuid(NullableWrapper<String> guid) {
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
            device.setNetwork(network.getValue());
        }
        if (status != null) {
            device.setStatus(status.getValue());
        }
        return device;
    }
}
