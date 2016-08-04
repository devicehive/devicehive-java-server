package com.devicehive.vo;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.Device;
import com.devicehive.model.User;

import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class NetworkWithUsersAndDevicesVO extends NetworkVO {

    private Set<User> users;

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

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    public Set<DeviceVO> getDevices() {
        return devices;
    }

    public void setDevices(Set<DeviceVO> devices) {
        this.devices = devices;
    }
}
