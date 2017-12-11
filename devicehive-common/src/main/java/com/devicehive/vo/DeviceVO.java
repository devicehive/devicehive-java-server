package com.devicehive.vo;

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
import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class DeviceVO implements HiveEntity {
    private static final long serialVersionUID = 1523137252183988289L;
    
    @SerializedName("sid")
    @ApiModelProperty(hidden = true)
    private Long id;

    @SerializedName("id")
    @ApiModelProperty(name = "id", dataType = "integer")
    @JsonPolicyDef({DEVICE_PUBLISHED, NETWORK_PUBLISHED, DEVICES_LISTED})
    private String deviceId;

    @NotNull
    @Size(min = 1, max = 128, message = "Field name cannot be empty. The length of name should not be more than 128 symbols.")
    @SerializedName("name")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICES_LISTED})
    private String name;

    @SerializedName("data")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICES_LISTED})
    private JsonStringWrapper data;

    @SerializedName("networkId")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, DEVICES_LISTED})
    private Long networkId;

    @SerializedName("isBlocked")
    @ApiModelProperty(name="isBlocked")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICES_LISTED})
    private Boolean blocked;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JsonStringWrapper getData() {
        return data;
    }

    public void setData(JsonStringWrapper data) {
        this.data = data;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public void setNetworkId(Long networkId) {
        this.networkId = networkId;
    }

    public Boolean getBlocked() {
        return blocked;
    }

    public void setBlocked(Boolean blocked) {
        this.blocked = blocked;
    }
}
