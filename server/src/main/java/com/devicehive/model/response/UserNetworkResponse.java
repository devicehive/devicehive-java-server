package com.devicehive.model.response;

import com.google.gson.annotations.SerializedName;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.Network;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NETWORKS_LISTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.USER_PUBLISHED;

//TODO: javadoc
public class UserNetworkResponse implements HiveEntity {

    private static final long serialVersionUID = 4328574005902197119L;
    @SerializedName("network")
    @JsonPolicyDef({USER_PUBLISHED, NETWORKS_LISTED})
    private Network network;

    public static UserNetworkResponse fromNetwork(Network network) {
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
