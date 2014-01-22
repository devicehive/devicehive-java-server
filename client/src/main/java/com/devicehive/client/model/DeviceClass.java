package com.devicehive.client.model;

import com.devicehive.client.impl.json.strategies.JsonPolicyDef;

import java.util.Set;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.*;

/**
 * Represents a device class which holds meta-information about devices.
 * For more details see <a href="http://www.devicehive.com/restful#Reference/DeviceClass">DeviceClass</a>
 */
public class DeviceClass implements HiveEntity {
    private static final long serialVersionUID = 967472386318199376L;
    @JsonPolicyDef(
            {DEVICE_PUBLISHED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED, DEVICECLASS_SUBMITTED,
                    DEVICE_PUBLISHED_DEVICE_AUTH})
    private Long id;

    @JsonPolicyDef({DEVICECLASS_PUBLISHED})
    private NullableWrapper<Set<Equipment>> equipment;

    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED,
            DEVICE_PUBLISHED_DEVICE_AUTH})
    private NullableWrapper<String> name;

    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED,
            DEVICE_PUBLISHED_DEVICE_AUTH})
    private NullableWrapper<String> version;

    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED,
            DEVICE_PUBLISHED_DEVICE_AUTH})
    private NullableWrapper<Boolean> isPermanent;

    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED,
            DEVICE_PUBLISHED_DEVICE_AUTH})
    private NullableWrapper<Integer> offlineTimeout;

    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED,
            DEVICE_PUBLISHED_DEVICE_AUTH})
    private NullableWrapper<JsonStringWrapper> data;

    public DeviceClass() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getName() {
        return NullableWrapper.value(name);
    }

    public void setName(String name) {
        this.name = NullableWrapper.create(name);
    }

    public void removeName() {
        this.name = null;
    }

    public String getVersion() {
        return NullableWrapper.value(version);
    }

    public void setVersion(String version) {
        this.version = NullableWrapper.create(version);
    }

    public void removeVersion() {
        this.version = null;
    }

    public Boolean getPermanent() {
        return NullableWrapper.value(isPermanent);
    }

    public void setPermanent(Boolean permanent) {
        isPermanent = NullableWrapper.create(permanent);
    }

    public void removePermanent() {
        this.isPermanent = null;
    }

    public Integer getOfflineTimeout() {
        return NullableWrapper.value(offlineTimeout);
    }

    public void setOfflineTimeout(Integer offlineTimeout) {
        this.offlineTimeout = NullableWrapper.create(offlineTimeout);
    }

    public void removeOfflineTimeout() {
        this.offlineTimeout = null;
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
}
