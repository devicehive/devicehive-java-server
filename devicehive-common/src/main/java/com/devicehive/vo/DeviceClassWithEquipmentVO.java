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

import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICECLASS_PUBLISHED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_PUBLISHED;

/**
 *
 */
public class DeviceClassWithEquipmentVO extends DeviceClassVO {

    @JsonPolicyDef({DEVICECLASS_PUBLISHED, DEVICE_PUBLISHED})
    private Set<DeviceClassEquipmentVO> equipment;

    public Set<DeviceClassEquipmentVO> getEquipment() {
        return equipment;
    }

    public void setEquipment(Set<DeviceClassEquipmentVO> equipment) {
        this.equipment = equipment;
    }
}
