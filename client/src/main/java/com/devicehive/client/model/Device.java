package com.devicehive.client.model;

import com.devicehive.client.json.strategies.JsonPolicyDef;

import java.util.Set;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.*;

public class Device implements HiveEntity {

    private static final long serialVersionUID = -7498444232044147881L;
    @JsonPolicyDef({DEVICE_PUBLISHED, NETWORK_PUBLISHED})
    private String id;
    @JsonPolicyDef({DEVICE_SUBMITTED, DEVICE_PUBLISHED})
    private String key;
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private String name;
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private String status;
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private JsonStringWrapper data;
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED})
    private Network network;
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private DeviceClass deviceClass;
    @JsonPolicyDef(DEVICE_PUBLISHED)
    private Set<Equipment> equipment;

    public Device() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
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

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public DeviceClass getDeviceClass() {
        return deviceClass;
    }

    public void setDeviceClass(DeviceClass deviceClass) {
        this.deviceClass = deviceClass;
    }

    public Set<Equipment> getEquipment() {
        return equipment;
    }

    public void setEquipment(Set<Equipment> equipment) {
        this.equipment = equipment;
    }
}
