package com.devicehive.dao.riak.vo;

import com.basho.riak.client.api.annotations.RiakIndex;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.NetworkWithUsersAndDevicesVO;

public class NetworkVoRiak {

    private Long id;
    private String key;
    private String name;
    private String description;
    private Long entityVersion;

    public NetworkVoRiak() {}

    public NetworkVoRiak(NetworkVO vo) {
        id = vo.getId();
        key = vo.getKey();
        name = vo.getName();
        description = vo.getDescription();
        entityVersion = vo.getEntityVersion();
        if (vo instanceof NetworkWithUsersAndDevicesVO) {
            //todo: find if we should copy child entities

        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(Long entityVersion) {
        this.entityVersion = entityVersion;
    }

    @RiakIndex(name = "name")
    public String getNameSi() {
        return getName();
    }

    public NetworkVO convert() {
        NetworkVO vo = new NetworkVO();
        vo.setId(id);
        vo.setKey(key);
        vo.setName(name);
        vo.setDescription(description);
        vo.setEntityVersion(entityVersion);
        return vo;
    }
}
