package com.devicehive.model.view;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.NullableWrapper;
import com.devicehive.model.domain.Network;

import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class NetworkView implements HiveEntity {

    private static final long serialVersionUID = -4134073649300446791L;

    @JsonPolicyDef({DEVICE_PUBLISHED, USER_PUBLISHED, NETWORKS_LISTED, NETWORK_PUBLISHED, NETWORK_SUBMITTED})
    private Long id;

    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, USER_PUBLISHED, NETWORKS_LISTED, NETWORK_PUBLISHED})
    private NullableWrapper<String> key;

    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, USER_PUBLISHED, NETWORKS_LISTED, NETWORK_PUBLISHED})
    private NullableWrapper<String> name;

    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, USER_PUBLISHED, NETWORKS_LISTED, NETWORK_PUBLISHED})
    private NullableWrapper<String> description;

    @JsonPolicyDef(NETWORK_PUBLISHED)
    private Set<DeviceView> devices;

    public NetworkView() {
    }

    public NetworkView(Network network) {
        convertFrom(network);
    }

    public Set<DeviceView> getDevices() {
        return devices;
    }

    public void setDevices(Set<DeviceView> devices) {
        this.devices = devices;
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

    public NullableWrapper<String> getDescription() {
        return description;
    }

    public void setDescription(NullableWrapper<String> description) {
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Network convertTo() {
        Network network = new Network();
        network.setId(id);
        if (key != null) {
            network.setKey(key.getValue());
        }
        if (name != null) {
            network.setName(name.getValue());
        }
        if (description != null) {
            network.setDescription(description.getValue());
        }
        return network;
    }

    public void convertFrom(Network network) {
        if (network == null) {
            return;
        }
        id = network.getId();
        key = new NullableWrapper<>(network.getKey());
        name = new NullableWrapper<>(network.getName());
        description = new NullableWrapper<>(network.getDescription());
    }
}
