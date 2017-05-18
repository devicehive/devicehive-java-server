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
import com.devicehive.vo.DeviceClassVO;

import javax.validation.constraints.Size;
import java.util.Optional;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class DeviceClassUpdate implements HiveEntity {

    private static final long serialVersionUID = 967472386318199376L;

    @JsonPolicyDef(DEVICE_PUBLISHED)
    private Long id;

    @Size(min = 1, max = 128, message = "Field name cannot be empty. The length of name should not be more than 128 symbols.")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED})
    private String name;

    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED})
    private Boolean isPermanent;

    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED})
    private JsonStringWrapper data;

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public Optional<Boolean> getPermanent() {
        return Optional.ofNullable(isPermanent);
    }

    public void setPermanent(Boolean permanent) {
        isPermanent = permanent;
    }

    public Optional<JsonStringWrapper> getData() {
        return Optional.ofNullable(data);
    }

    public void setData(JsonStringWrapper data) {
        this.data = data;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DeviceClassVO convertTo() {
        DeviceClassVO deviceClass = new DeviceClassVO();
        if (this.id != null){
            deviceClass.setId(this.id);
        }
        if (this.isPermanent != null){
            deviceClass.setIsPermanent(this.isPermanent);
        }
        if (this.data != null){
            deviceClass.setData(this.data);
        }
        if (this.name != null){
            deviceClass.setName(this.name);
        }
        return deviceClass;
    }
}
