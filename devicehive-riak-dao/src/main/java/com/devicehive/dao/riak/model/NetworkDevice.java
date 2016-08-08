package com.devicehive.dao.riak.model;

import com.basho.riak.client.api.annotations.RiakIndex;

public class NetworkDevice {

    private String id;
    private Long networkId;
    private String deviceUuid;

    public NetworkDevice() {
    }

    public NetworkDevice(Long networkId, String deviceUuid) {
        this.networkId = networkId;
        this.deviceUuid = deviceUuid;
    }

    @RiakIndex(name = "networkId")
    public Long getNetworkSi() {
        return networkId;
    }

    @RiakIndex(name = "deviceUuid")
    public String getDeviceSi() {
        return deviceUuid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public void setNetworkId(Long networkId) {
        this.networkId = networkId;
    }

    public String getDeviceUuid() {
        return deviceUuid;
    }

    public void setDeviceUuid(String deviceUuid) {
        this.deviceUuid = deviceUuid;
    }
}
