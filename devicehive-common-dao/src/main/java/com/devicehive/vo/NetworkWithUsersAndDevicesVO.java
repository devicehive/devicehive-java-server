package com.devicehive.vo;

import com.devicehive.json.strategies.JsonPolicyDef;

import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NETWORK_PUBLISHED;

public class NetworkWithUsersAndDevicesVO extends NetworkVO {

    private Set<UserVO> users;

    @JsonPolicyDef({NETWORK_PUBLISHED})
    private Set<DeviceVO> devices;

    public NetworkWithUsersAndDevicesVO() {}

    public NetworkWithUsersAndDevicesVO(NetworkVO vo) {
        setId(vo.getId());
        setKey(vo.getKey());
        setName(vo.getName());
        setDescription(vo.getDescription());
        setEntityVersion(vo.getEntityVersion());
    }

    public Set<UserVO> getUsers() {
        return users;
    }

    public void setUsers(Set<UserVO> users) {
        this.users = users;
    }

    public Set<DeviceVO> getDevices() {
        return devices;
    }

    public void setDevices(Set<DeviceVO> devices) {
        this.devices = devices;
    }
}
