package com.devicehive.model.response;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.devicehive.vo.NetworkVO;
import com.google.gson.annotations.SerializedName;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NETWORKS_LISTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.USER_PUBLISHED;

//TODO: javadoc
public class UserNetworkResponse implements HiveEntity {

    private static final long serialVersionUID = 4328574005902197119L;
    @SerializedName("network")
    @JsonPolicyDef({USER_PUBLISHED, NETWORKS_LISTED})
    private NetworkVO network;

    public static UserNetworkResponse fromNetwork(NetworkVO network) {
        UserNetworkResponse result = new UserNetworkResponse();
        result.setNetwork(network);
        return result;
    }

    public NetworkVO getNetwork() {
        return network;
    }

    public void setNetwork(NetworkVO network) {
        this.network = network;
    }
}
