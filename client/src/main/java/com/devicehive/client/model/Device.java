package com.devicehive.client.model;

import com.devicehive.client.impl.json.strategies.JsonPolicyDef;

import java.util.Set;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.*;

/**
 * Represents a device, a unit that runs microcode and communicates to this API.
 * For more details see <a href="http://www.devicehive.com/restful#Reference/Device">Device</a>
 */
public class Device implements HiveEntity {

    private static final long serialVersionUID = -7498444232044147881L;
    @JsonPolicyDef({DEVICE_PUBLISHED, NETWORK_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH})
    private String id;

    @JsonPolicyDef({DEVICE_SUBMITTED, DEVICE_PUBLISHED})
    private NullableWrapper<String> key;

    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH})
    private NullableWrapper<String> name;

    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH})
    private NullableWrapper<String> status;

    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH})
    private NullableWrapper<JsonStringWrapper> data;

    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, DEVICE_PUBLISHED_DEVICE_AUTH})
    private NullableWrapper<Network> network;

    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH})
    private NullableWrapper<DeviceClass> deviceClass;

    @JsonPolicyDef(DEVICE_PUBLISHED)
    private NullableWrapper<Set<Equipment>> equipment;

    public Device() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return NullableWrapper.value(key);
    }

    public void setKey(String key) {
        this.key = NullableWrapper.create(key);
    }

    public void removeKey() {
        this.key = null;
    }

    public String getName() {
        return NullableWrapper.value(name);
    }

    public void setName(String name) {
        this.name = NullableWrapper.create(name);
    }

    public void removeName() {
        this.name = null;
    }

    public String getStatus() {
        return NullableWrapper.value(status);
    }

    public void setStatus(String status) {
        this.status = NullableWrapper.create(status);
    }

    public void removeStatus() {
        this.status = null;
    }

    public JsonStringWrapper getData() {
        return NullableWrapper.value(data);
    }

    public void setData(JsonStringWrapper data) {
        this.data = NullableWrapper.create(data);
    }

    public void removeData() {
        this.data = null;
    }

    public Network getNetwork() {
        return NullableWrapper.value(network);
    }

    public void setNetwork(Network network) {
        this.network = NullableWrapper.create(network);
    }

    public void removeNetwork() {
        this.network = null;
    }

    public DeviceClass getDeviceClass() {
        return NullableWrapper.value(deviceClass);
    }

    public void setDeviceClass(DeviceClass deviceClass) {
        this.deviceClass = NullableWrapper.create(deviceClass);
    }

    public void removeDeviceClass() {
        this.deviceClass = null;
    }

    public Set<Equipment> getEquipment() {
        return NullableWrapper.value(equipment);
    }

    public void setEquipment(Set<Equipment> equipment) {
        this.equipment = NullableWrapper.create(equipment);
    }

    public void removeEquipment() {
        this.equipment = null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Device{");
        sb.append("name=").append(name);
        sb.append(", status=").append(status);
        sb.append(", data=").append(data);
        sb.append(", network=").append(network);
        sb.append(", deviceClass=").append(deviceClass);
        sb.append(", equipment=").append(equipment);
        sb.append(", id='").append(id).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
