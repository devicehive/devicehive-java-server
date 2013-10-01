package com.devicehive.client.model;

import com.devicehive.client.json.strategies.JsonPolicyDef;

import java.util.Set;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.*;

public class Network implements HiveEntity {

    private static final long serialVersionUID = -4134073649300446791L;

    @JsonPolicyDef({DEVICE_PUBLISHED, USER_PUBLISHED, NETWORKS_LISTED, NETWORK_PUBLISHED, NETWORK_SUBMITTED})
    private Long id;

    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, USER_PUBLISHED, NETWORKS_LISTED, NETWORK_PUBLISHED})
    private String key;

    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, USER_PUBLISHED, NETWORKS_LISTED, NETWORK_PUBLISHED})
    private String name;

    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, USER_PUBLISHED, NETWORKS_LISTED, NETWORK_PUBLISHED})
    private String description;

    @JsonPolicyDef(NETWORK_PUBLISHED)
    private Set<Device> devices;

    public Network() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<Device> getDevices() {
        return devices;
    }

    public void setDevices(Set<Device> devices) {
        this.devices = devices;
    }
}
