package com.devicehive.vo;

import com.devicehive.json.strategies.JsonPolicyDef;
import io.swagger.annotations.ApiModelProperty;

import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_TYPE_PUBLISHED;

public class DeviceTypeWithUsersAndDevicesVO extends DeviceTypeVO {

    @ApiModelProperty(hidden = true)
    private Set<UserVO> users;

    @JsonPolicyDef({DEVICE_TYPE_PUBLISHED})
    private Set<DeviceVO> devices;

    public DeviceTypeWithUsersAndDevicesVO() {}

    public DeviceTypeWithUsersAndDevicesVO(DeviceTypeVO vo) {
        setId(vo.getId());
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
