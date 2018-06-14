package com.devicehive.model.rpc;

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

import com.devicehive.shim.api.Action;
import com.devicehive.shim.api.Body;
import com.devicehive.vo.DeviceVO;

import java.util.Set;

public class DeviceTypeDeleteRequest extends Body {

    private Long deviceTypeId;
    private Set<DeviceVO> devices;

    public DeviceTypeDeleteRequest(Long deviceTypeId, Set<DeviceVO> devices) {
        super(Action.DEVICE_TYPE_DELETE_REQUEST);
        this.deviceTypeId = deviceTypeId;
        this.devices = devices;
    }

    public Long getDeviceTypeId() {
        return deviceTypeId;
    }

    public void setDeviceTypeId(Long deviceTypeId) {
        this.deviceTypeId = deviceTypeId;
    }

    public Set<DeviceVO> getDevices() {
        return devices;
    }

    public void setDevices(Set<DeviceVO> devices) {
        this.devices = devices;
    }

    @Override
    public String toString() {
        return "DeviceTypeDeleteRequest{" +
                "deviceTypeId=" + deviceTypeId +
                ", devices=" + devices +
                '}';
    }
}
