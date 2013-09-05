package com.devicehive.model.view;

import com.devicehive.json.strategies.JsonPolicyDef;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NETWORKS_LISTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.USER_PUBLISHED;

public class UserNetworkView implements HiveEntity {

    @JsonPolicyDef({NETWORKS_LISTED, USER_PUBLISHED})
    private NetworkView network;

    public UserNetworkView() {
    }


    public NetworkView getNetwork() {
        return network;
    }

    public void setNetwork(NetworkView network) {
        this.network = network;
    }
}
