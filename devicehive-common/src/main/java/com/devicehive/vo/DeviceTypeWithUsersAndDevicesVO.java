package com.devicehive.vo;

/*
 * #%L
 * DeviceHive Common Module
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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
