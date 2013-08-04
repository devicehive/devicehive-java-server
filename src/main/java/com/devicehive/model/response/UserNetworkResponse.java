package com.devicehive.model.response;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.Network;
import com.google.gson.annotations.SerializedName;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NETWORKS_LISTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.USER_PUBLISHED;

//TODO: javadoc
public class UserNetworkResponse implements HiveEntity {

    @SerializedName("network")
    @JsonPolicyDef({USER_PUBLISHED, NETWORKS_LISTED})
    private Network network;

    public static UserNetworkResponse fromNetwork(Network network){
        UserNetworkResponse result = new UserNetworkResponse();
        result.setNetwork(network);
        return result;
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }
}
