package com.devicehive.model.updates;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.Device;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.NullableWrapper;

import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class NetworkUpdate implements HiveEntity {
    private static final long serialVersionUID = -4134073649300446791L;

    @JsonPolicyDef({DEVICE_PUBLISHED, USER_PUBLISHED, NETWORKS_LISTED, NETWORK_PUBLISHED})
    private Long id;
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, USER_PUBLISHED, NETWORKS_LISTED, NETWORK_PUBLISHED})
    private NullableWrapper<String> key;
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, USER_PUBLISHED, NETWORKS_LISTED, NETWORK_PUBLISHED})
    private NullableWrapper<String> name;
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, USER_PUBLISHED, NETWORKS_LISTED, NETWORK_PUBLISHED})
    private NullableWrapper<String> description;
    @JsonPolicyDef({NETWORK_PUBLISHED})
    private Set<Device> devices;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public NullableWrapper<String> getDescription() {
        return description;
    }

    public void setDescription(NullableWrapper<String> description) {
        this.description = description;
    }
}
