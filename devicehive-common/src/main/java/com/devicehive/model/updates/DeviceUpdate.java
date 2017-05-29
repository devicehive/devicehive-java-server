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
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Size;
import java.util.Optional;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class DeviceUpdate implements HiveEntity {

    private static final long serialVersionUID = -7498444232044147881L;

    @SerializedName("id")
    @JsonPolicyDef({DEVICE_PUBLISHED, NETWORK_PUBLISHED})
    private String guid;

    @Size(min = 1, max = 128, message = "Field name cannot be empty. The length of name should not be more than 128 symbols.")
    @SerializedName("name")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private String name;

    @SerializedName("data")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private JsonStringWrapper data;

    @SerializedName("network")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED})
    private NetworkVO network;

    @JsonPolicyDef({DEVICE_SUBMITTED, DEVICE_PUBLISHED})
    @SerializedName("isBlocked")
    private Boolean blocked;

    public Optional<String> getGuid() {
        return Optional.ofNullable(guid);
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

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

    public Optional<NetworkVO> getNetwork() {
        return Optional.ofNullable(network);
    }

    public void setNetwork(NetworkVO network) {
        this.network = network;
    }

    public Optional<Boolean> getBlocked() {
        return Optional.ofNullable(blocked);
    }

    public void setBlocked(Boolean blocked) {
        this.blocked = blocked;
    }

    public DeviceVO convertTo() {
        DeviceVO device = new DeviceVO();
        if (this.guid != null){
            device.setGuid(this.guid);
        }
        if (this.data != null){
            device.setData(this.data);
        }
        if (this.name != null){
            device.setName(this.name);
        }
        if (this.network != null){
            device.setNetwork(this.network);
        }
        if (this.blocked != null){
            device.setBlocked(this.blocked);
        }
        return device;
    }
}
