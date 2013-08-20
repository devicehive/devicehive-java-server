package com.devicehive.model.request;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.Device;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.NullableWrapper;
import com.devicehive.model.User;
import com.google.gson.annotations.SerializedName;

import java.util.Set;

//TODO: javadoc
public class NetworkRequest implements HiveEntity {

    private static final long serialVersionUID = -4134073649300446791L;
    @SerializedName("id")
    @JsonPolicyDef({JsonPolicyDef.Policy.DEVICE_PUBLISHED, JsonPolicyDef.Policy.USER_PUBLISHED,
            JsonPolicyDef.Policy.NETWORKS_LISTED, JsonPolicyDef.Policy.NETWORK_PUBLISHED})
    private Long id;

    @SerializedName("key")
    @JsonPolicyDef({JsonPolicyDef.Policy.DEVICE_PUBLISHED, JsonPolicyDef.Policy.DEVICE_SUBMITTED,
            JsonPolicyDef.Policy.USER_PUBLISHED, JsonPolicyDef.Policy.NETWORKS_LISTED, JsonPolicyDef.Policy.NETWORK_PUBLISHED})
    private NullableWrapper<String> key;

    @SerializedName("name")
    @JsonPolicyDef({JsonPolicyDef.Policy.DEVICE_PUBLISHED, JsonPolicyDef.Policy.DEVICE_SUBMITTED,
            JsonPolicyDef.Policy.USER_PUBLISHED, JsonPolicyDef.Policy.NETWORKS_LISTED, JsonPolicyDef.Policy.NETWORK_PUBLISHED})
    private NullableWrapper<String> name;

    @SerializedName("description")
    @JsonPolicyDef({JsonPolicyDef.Policy.DEVICE_PUBLISHED, JsonPolicyDef.Policy.DEVICE_SUBMITTED,
            JsonPolicyDef.Policy.USER_PUBLISHED, JsonPolicyDef.Policy.NETWORKS_LISTED, JsonPolicyDef.Policy.NETWORK_PUBLISHED})
    private NullableWrapper<String> description;


    private Set<User> users;

    @JsonPolicyDef({JsonPolicyDef.Policy.NETWORK_PUBLISHED})
    private Set<Device> devices;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
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
