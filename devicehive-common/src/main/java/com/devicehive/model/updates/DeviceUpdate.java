package com.devicehive.model.updates;

/*
 * #%L
 * DeviceHive Common Dao Interfaces
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


import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.vo.DeviceVO;
import com.google.gson.annotations.SerializedName;

import javax.validation.constraints.Size;
import java.util.Optional;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class DeviceUpdate implements HiveEntity {

    private static final long serialVersionUID = -7498444232044147881L;

    @Size(min = 1, max = 128, message = "Field name cannot be empty. The length of name should not be more than 128 symbols.")
    @SerializedName("name")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICE_TYPE_PUBLISHED})
    private String name;

    @SerializedName("data")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICE_TYPE_PUBLISHED})
    private JsonStringWrapper data;

    @SerializedName("networkId")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED})
    private Long networkId;

    @SerializedName("deviceTypeId")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED})
    private Long deviceTypeId;

    @JsonPolicyDef({DEVICE_SUBMITTED, DEVICE_PUBLISHED})
    @SerializedName("isBlocked")
    private Boolean blocked;

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public Optional<JsonStringWrapper> getData() {
        return Optional.ofNullable(data);
    }

    public void setData(JsonStringWrapper data) {
        this.data = data;
    }

    public Optional<Long> getNetworkId() {
        return Optional.ofNullable(networkId);
    }

    public void setNetworkId(Long networkId) {
        this.networkId = networkId;
    }

    public Optional<Long> getDeviceTypeId() {
        return Optional.ofNullable(deviceTypeId);
    }

    public void setDeviceTypeId(Long deviceTypeId) {
        this.deviceTypeId = deviceTypeId;
    }

    public Optional<Boolean> getBlocked() {
        return Optional.ofNullable(blocked);
    }

    public void setBlocked(Boolean blocked) {
        this.blocked = blocked;
    }

    public DeviceVO convertTo(String deviceId) {
        DeviceVO device = new DeviceVO();
        if (deviceId != null){
            device.setDeviceId(deviceId);
        }
        if (this.data != null){
            device.setData(this.data);
        }
        if (this.name != null){
            device.setName(this.name);
        }
        if (this.networkId != null){
            device.setNetworkId(this.networkId);
        }
        if (this.deviceTypeId != null){
            device.setDeviceTypeId(this.deviceTypeId);
        }
        if (this.blocked != null){
            device.setBlocked(this.blocked);
        }
        return device;
    }
}
