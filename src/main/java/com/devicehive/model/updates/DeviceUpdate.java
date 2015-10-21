package com.devicehive.model.updates;


import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.*;
import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Optional;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

@ApiModel
public class DeviceUpdate implements HiveEntity {

    private static final long serialVersionUID = -7498444232044147881L;
    @SerializedName("id")
    @JsonPolicyDef({DEVICE_PUBLISHED, NETWORK_PUBLISHED})
    @ApiModelProperty(dataType = "string")
    private Optional<String> guid;

    @SerializedName("key")
    @JsonPolicyDef({DEVICE_SUBMITTED, DEVICE_PUBLISHED})
    @ApiModelProperty(dataType = "string")
    private Optional<String> key;

    @SerializedName("name")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    @ApiModelProperty(dataType = "string")
    private Optional<String> name;

    @SerializedName("status")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    @ApiModelProperty(dataType = "string")
    private Optional<String> status;

    @SerializedName("data")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    @ApiModelProperty(dataType = "com.devicehive.model.JsonStringWrapper")
    private Optional<JsonStringWrapper> data;

    @SerializedName("network")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED})
    @ApiModelProperty(dataType = "com.devicehive.model.Network")
    private Optional<Network> network;

    @SerializedName("deviceClass")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    @ApiModelProperty(dataType = "com.devicehive.model.updates.DeviceClassUpdate")
    private Optional<DeviceClassUpdate> deviceClass;

    @JsonPolicyDef({DEVICE_SUBMITTED, DEVICE_PUBLISHED})
    @SerializedName("blocked")
    @ApiModelProperty(dataType = "boolean")
    private Optional<Boolean> blocked;

    public Optional<DeviceClassUpdate> getDeviceClass() {
        return deviceClass;
    }

    public void setDeviceClass(Optional<DeviceClassUpdate> deviceClass) {
        this.deviceClass = deviceClass;
    }

    public Optional<String> getGuid() {
        return guid;
    }

    public void setGuid(Optional<String> guid) {
        this.guid = guid;
    }

    public Optional<String> getKey() {
        return key;
    }

    public void setKey(Optional<String> key) {
        this.key = key;
    }

    public Optional<String> getName() {
        return name;
    }

    public void setName(Optional<String> name) {
        this.name = name;
    }

    public Optional<String> getStatus() {
        return status;
    }

    public void setStatus(Optional<String> status) {
        this.status = status;
    }

    public Optional<JsonStringWrapper> getData() {
        return data;
    }

    public void setData(Optional<JsonStringWrapper> data) {
        this.data = data;
    }

    public Optional<Network> getNetwork() {
        return network;
    }

    public void setNetwork(Optional<Network> network) {
        this.network = network;
    }

    public Optional<Boolean> getBlocked() {
        return blocked;
    }

    public void setBlocked(Optional<Boolean> blocked) {
        this.blocked = blocked;
    }

    public Device convertTo() {
        Device device = new Device();
        if (guid != null) {
            device.setGuid(guid.orElse(null));
        }
        if (data != null) {
            device.setData(data.orElse(null));
        }
        if (deviceClass != null) {
            DeviceClass convertedDeviceClass = deviceClass.orElse(null).convertTo();
            device.setDeviceClass(convertedDeviceClass);
        }
        if (key != null) {
            device.setKey(key.orElse(null));
        }
        if (name != null) {
            device.setName(name.orElse(null));
        }
        if (network != null) {
            device.setNetwork(network.orElse(null));
        }
        if (status != null) {
            device.setStatus(status.orElse(null));
        }
        if (blocked != null) {
            device.setBlocked(Boolean.TRUE.equals(blocked.orElse(null)));
        }
        return device;
    }
}
