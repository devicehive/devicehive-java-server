package com.devicehive.vo;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.JsonStringWrapper;
import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModelProperty;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class DeviceVO implements HiveEntity {

    @SerializedName("sid")
    private Long id;

    @SerializedName("id")
    @JsonPolicyDef({DEVICE_PUBLISHED, NETWORK_PUBLISHED})
    private String guid;

    @SerializedName("name")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private String name;

    @SerializedName("status")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private String status;

    @SerializedName("data")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private JsonStringWrapper data;

    @SerializedName("network")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED})
    private NetworkVO network;

    @SerializedName("deviceClass")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private DeviceClassVO deviceClass;

    @SerializedName("isBlocked")
    @ApiModelProperty(name="isBlocked")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private Boolean blocked;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public JsonStringWrapper getData() {
        return data;
    }

    public void setData(JsonStringWrapper data) {
        this.data = data;
    }

    public NetworkVO getNetwork() {
        return network;
    }

    public void setNetwork(NetworkVO network) {
        this.network = network;
    }

    public DeviceClassVO getDeviceClass() {
        return deviceClass;
    }

    public void setDeviceClass(DeviceClassVO deviceClass) {
        this.deviceClass = deviceClass;
    }

    public Boolean getBlocked() {
        return blocked;
    }

    public void setBlocked(Boolean blocked) {
        this.blocked = blocked;
    }
}
