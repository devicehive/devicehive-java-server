package com.devicehive.model.rpc;

import com.devicehive.shim.api.Body;
import com.devicehive.vo.NetworkVO;

import java.util.List;

public class ListNetworkResponse extends Body {

    private List<NetworkVO> networks;

    public ListNetworkResponse(List<NetworkVO> networks) {
        super(Action.LIST_NETWORK_RESPONSE.name());
        this.networks = networks;
    }

    public List<NetworkVO> getNetworks() {
        return networks;
    }
}
