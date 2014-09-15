package com.devicehive.client.model;

import com.devicehive.client.impl.json.strategies.JsonPolicyDef;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.DEVICE_PUBLISHED;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.DEVICE_PUBLISHED_DEVICE_AUTH;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.DEVICE_SUBMITTED;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.NETWORKS_LISTED;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.NETWORK_PUBLISHED;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.NETWORK_SUBMITTED;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.NETWORK_UPDATE;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.USER_PUBLISHED;

/**
 * Represents a network, an isolated area where devices reside. For more details see <a
 * href="http://www.devicehive.com/restful#Reference/Network">Network</a>
 */
public class Network implements HiveEntity {

    private static final long serialVersionUID = -4134073649300446791L;
    @JsonPolicyDef({DEVICE_PUBLISHED, USER_PUBLISHED, NETWORKS_LISTED, NETWORK_PUBLISHED, NETWORK_SUBMITTED,
                    DEVICE_PUBLISHED_DEVICE_AUTH})
    private Long id;

    @JsonPolicyDef(
        {DEVICE_PUBLISHED, DEVICE_SUBMITTED, USER_PUBLISHED, NETWORKS_LISTED, NETWORK_PUBLISHED, NETWORK_UPDATE})
    private NullableWrapper<String> key;

    @JsonPolicyDef(
        {DEVICE_PUBLISHED, DEVICE_SUBMITTED, USER_PUBLISHED, NETWORKS_LISTED, NETWORK_PUBLISHED, NETWORK_UPDATE,
         DEVICE_PUBLISHED_DEVICE_AUTH})
    private NullableWrapper<String> name;

    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, USER_PUBLISHED, NETWORKS_LISTED, NETWORK_PUBLISHED,
                    NETWORK_UPDATE, DEVICE_PUBLISHED_DEVICE_AUTH})
    private NullableWrapper<String> description;


    public Network() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public String getDescription() {
        return NullableWrapper.value(description);
    }

    public void setDescription(String description) {
        this.description = NullableWrapper.create(description);
    }

    public void removeDescription() {
        this.description = null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Network{");
        sb.append("id=").append(id);
        sb.append(", name=").append(name);
        sb.append(", description=").append(description);
        sb.append('}');
        return sb.toString();
    }
}
