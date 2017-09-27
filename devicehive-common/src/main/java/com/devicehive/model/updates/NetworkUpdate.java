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
import com.devicehive.vo.NetworkVO;

import javax.validation.constraints.Size;
import java.util.Optional;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class NetworkUpdate implements HiveEntity {

    private static final long serialVersionUID = -4134073649300446791L;

    @Size(min = 1, max = 128, message = "Field name cannot be empty. The length of name should not be more than 128 symbols.")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, USER_PUBLISHED, NETWORKS_LISTED, NETWORK_PUBLISHED})
    private String name;

    @Size(max = 128, message = "The length of description should not be more than 128 symbols.")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, USER_PUBLISHED, NETWORKS_LISTED, NETWORK_PUBLISHED})
    private String description;

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public NetworkVO convertTo() {
        NetworkVO networkVO = new NetworkVO();
        networkVO.setName(name);
        networkVO.setDescription(description);
        
        return networkVO;
    }
}
