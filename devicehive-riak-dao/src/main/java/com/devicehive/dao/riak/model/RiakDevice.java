package com.devicehive.dao.riak.model;

import com.basho.riak.client.api.annotations.RiakIndex;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.Network;
import com.devicehive.vo.DeviceVO;

public class RiakDevice {

    private Long id;

    private String guid;

    private String name;

    private String status;

    private JsonStringWrapper data;

    private Network network;

    private DeviceClass deviceClass;

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

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
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

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public DeviceClass getDeviceClass() {
        return deviceClass;
    }

    public void setDeviceClass(DeviceClass deviceClass) {
        this.deviceClass = deviceClass;
    }

    public Boolean getBlocked() {
        return blocked;
    }

    public void setBlocked(Boolean blocked) {
        this.blocked = blocked;
    }

    //Riak indexes
    @RiakIndex(name = "guid")
    public String getGuidSi() {
        return guid;
    }

    public static DeviceVO convertToVo(RiakDevice dc) {
        DeviceVO vo = null;
        if (dc != null) {
            vo = new DeviceVO();
            vo.setBlocked(dc.getBlocked());
            vo.setData(dc.getData());
            //TODO ???vo.setDeviceClass();
            vo.setGuid(dc.getGuid());
            vo.setId(dc.getId());
            vo.setName(dc.getName());
            //TODO ???vo.setNetwork();
            vo.setStatus(dc.getStatus());
        }
        return vo;
    }

    public static RiakDevice convertToEntity(DeviceVO dc) {
        RiakDevice entity = null;
        if (dc != null) {
            entity = new RiakDevice();
            entity.setBlocked(dc.getBlocked());
            entity.setData(dc.getData());
            entity.setDeviceClass(dc.getDeviceClass());
            entity.setGuid(dc.getGuid());
            entity.setId(dc.getId());
            entity.setName(dc.getName());
            entity.setNetwork(dc.getNetwork());
            entity.setStatus(dc.getStatus());
        }
        return entity;
    }

}
