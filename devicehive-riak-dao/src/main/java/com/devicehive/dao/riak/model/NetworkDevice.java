package com.devicehive.dao.riak.model;

/*
 * #%L
 * DeviceHive Dao Riak Implementation
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
