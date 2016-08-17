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
import com.devicehive.vo.DeviceClassEquipmentVO;
import com.devicehive.vo.DeviceClassWithEquipmentVO;

import java.util.Optional;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class DeviceClassUpdate implements HiveEntity {

    private static final long serialVersionUID = 967472386318199376L;
    @JsonPolicyDef(DEVICE_PUBLISHED)
    private Long id;
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED})
    private Optional<String> name;
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED})
    private Optional<Boolean> isPermanent;
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED})
    private Optional<Integer> offlineTimeout;
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED})
    private Optional<JsonStringWrapper> data;
    @JsonPolicyDef({DEVICECLASS_PUBLISHED, DEVICE_SUBMITTED})
    private Optional<Set<DeviceClassEquipmentVO>> equipment;

    public Optional<Set<DeviceClassEquipmentVO>> getEquipment() {
        return equipment;
    }

    public void setEquipment(Optional<Set<DeviceClassEquipmentVO>> equipment) {
        this.equipment = equipment;
    }

    public Optional<String> getName() {
        return name;
    }

    public void setName(Optional<String> name) {
        this.name = name;
    }

    public Optional<Boolean> getPermanent() {
        return isPermanent;
    }

    public void setPermanent(Optional<Boolean> permanent) {
        isPermanent = permanent;
    }

    public Optional<Integer> getOfflineTimeout() {
        return offlineTimeout;
    }

    public void setOfflineTimeout(Optional<Integer> offlineTimeout) {
        this.offlineTimeout = offlineTimeout;
    }

    public Optional<JsonStringWrapper> getData() {
        return data;
    }

    public void setData(Optional<JsonStringWrapper> data) {
        this.data = data;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DeviceClassWithEquipmentVO convertTo() {
        DeviceClassWithEquipmentVO deviceClass = new DeviceClassWithEquipmentVO();
        deviceClass.setId(id);
        if (isPermanent != null) {
            deviceClass.setIsPermanent(isPermanent.orElse(null));
        }
        if (offlineTimeout != null) {
            deviceClass.setOfflineTimeout(offlineTimeout.orElse(null));
        }
        if (data != null) {
            deviceClass.setData(data.orElse(null));
        }
        if (name != null) {
            deviceClass.setName(name.orElse(null));
        }
        if (equipment != null) {
            deviceClass.setEquipment(equipment.orElse(null));
        }
        return deviceClass;
    }
}
