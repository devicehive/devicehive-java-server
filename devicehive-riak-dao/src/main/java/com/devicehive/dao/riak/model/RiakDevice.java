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
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.vo.DeviceVO;

public class RiakDevice {

    private Long id;

    private String deviceId;

    private String name;

    private String status;

    private JsonStringWrapper data;

    private RiakNetwork network;

    private Boolean blocked;

    public JsonStringWrapper getData() {
        return data;
    }

    public void setData(JsonStringWrapper data) {
        this.data = data;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public RiakNetwork getNetwork() {
        return network;
    }

    public void setNetwork(RiakNetwork network) {
        this.network = network;
    }

    public Boolean getBlocked() {
        return blocked;
    }

    public void setBlocked(Boolean blocked) {
        this.blocked = blocked;
    }

    //Riak indexes
    @RiakIndex(name = "deviceId")
    public String getGuidSi() {
        return deviceId;
    }

    public static DeviceVO convertToVo(RiakDevice dc) {
        DeviceVO vo = null;
        if (dc != null) {
            vo = new DeviceVO();
            vo.setBlocked(dc.getBlocked());
            vo.setData(dc.getData());
            vo.setDeviceId(dc.getDeviceId());
            vo.setId(dc.getId());
            vo.setName(dc.getName());
            vo.setNetworkId(dc.getNetwork().getId());
        }
        return vo;
    }

    public static RiakDevice convertToEntity(DeviceVO dc) {
        RiakDevice entity = null;
        if (dc != null) {
            entity = new RiakDevice();
            entity.setBlocked(dc.getBlocked());
            entity.setData(dc.getData());
            entity.setDeviceId(dc.getDeviceId());
            entity.setId(dc.getId());
            entity.setName(dc.getName());
            RiakNetwork network = new RiakNetwork();
            network.setId(dc.getNetworkId());
            entity.setNetwork(network);
        }
        return entity;
    }

}
