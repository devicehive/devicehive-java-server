package com.devicehive.client.model;

import com.devicehive.client.json.strategies.JsonPolicyDef;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.NETWORKS_LISTED;
import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.USER_PUBLISHED;

/**
 * User-Network association
 */
public class UserNetwork implements HiveEntity {

    @JsonPolicyDef({NETWORKS_LISTED, USER_PUBLISHED})
    private Network network;

    public UserNetwork() {
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }
}
