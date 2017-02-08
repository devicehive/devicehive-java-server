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
import com.devicehive.model.*;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.NetworkVO;
import com.google.gson.annotations.SerializedName;

import java.util.Optional;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class DeviceUpdate implements HiveEntity {

    private static final long serialVersionUID = -7498444232044147881L;
    @SerializedName("id")
    @JsonPolicyDef({DEVICE_PUBLISHED, NETWORK_PUBLISHED})
    private Optional<String> guid;

    @SerializedName("name")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private Optional<String> name;

    @SerializedName("status")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private Optional<String> status;

    @SerializedName("data")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private Optional<JsonStringWrapper> data;

    @SerializedName("network")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED})
    private Optional<NetworkVO> network;

    @SerializedName("deviceClass")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private Optional<DeviceClassUpdate> deviceClass;

    @JsonPolicyDef({DEVICE_SUBMITTED, DEVICE_PUBLISHED})
    @SerializedName("isBlocked")
    private Optional<Boolean> blocked;

    public Optional<DeviceClassUpdate> getDeviceClass() {
        return deviceClass;
    }

    public void setDeviceClass(Optional<DeviceClassUpdate> deviceClass) {
        this.deviceClass = deviceClass;
    }

    public Optional<String> getGuid() {
        return guid;
    }

    public void setGuid(Optional<String> guid) {
        this.guid = guid;
    }

    public Optional<String> getName() {
        return name;
    }

    public void setName(Optional<String> name) {
        this.name = name;
    }

    public Optional<String> getStatus() {
        return status;
    }

    public void setStatus(Optional<String> status) {
        this.status = status;
    }

    public Optional<JsonStringWrapper> getData() {
        return data;
    }

    public void setData(Optional<JsonStringWrapper> data) {
        this.data = data;
    }

    public Optional<NetworkVO> getNetwork() {
        return network;
    }

    public void setNetwork(Optional<NetworkVO> network) {
        this.network = network;
    }

    public Optional<Boolean> getBlocked() {
        return blocked;
    }

    public void setBlocked(Optional<Boolean> blocked) {
        this.blocked = blocked;
    }

    public DeviceVO convertTo() {
        DeviceVO device = new DeviceVO();
        if (guid != null) {
            device.setGuid(guid.orElse(null));
        }
        if (data != null) {
            device.setData(data.orElse(null));
        }
        if (name != null) {
            device.setName(name.orElse(null));
        }
        if (network != null) {
            device.setNetwork(network.orElse(null));
        }
        if (status != null) {
            device.setStatus(status.orElse(null));
        }
        if (blocked != null) {
            device.setBlocked(Boolean.TRUE.equals(blocked.orElse(null)));
        }
        return device;
    }
}
