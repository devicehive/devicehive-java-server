package com.devicehive.model.view;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.domain.Network;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NETWORKS_LISTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.USER_PUBLISHED;

public class UserNetworkView implements HiveEntity {

    @JsonPolicyDef({NETWORKS_LISTED, USER_PUBLISHED})
    private NetworkView network;

    public UserNetworkView() {
    }

    public UserNetworkView(Network net){
        convertFrom(net);
    }

    public NetworkView getNetwork() {
        return network;
    }

    public void setNetwork(NetworkView network) {
        this.network = network;
    }

    public void convertFrom(Network net){
        network = new NetworkView(net);
    }

    public Network convertToNetwork(){
        return network.convertTo();
    }
}
