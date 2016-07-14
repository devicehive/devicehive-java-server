package com.devicehive.model;

import com.basho.riak.client.api.annotations.RiakIndex;

public class UserNetwork {

    private String id;

    private Long userId;

    private Long networkId;

    public UserNetwork() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public void setNetworkId(Long networkId) {
        this.networkId = networkId;
    }

    //Riak indexes
    @RiakIndex(name = "userId")
    public Long getUserIdSi() {
        return userId;
    }

    @RiakIndex(name = "networkId")
    public Long getNetworkIdSi() {
        return networkId;
    }
}
