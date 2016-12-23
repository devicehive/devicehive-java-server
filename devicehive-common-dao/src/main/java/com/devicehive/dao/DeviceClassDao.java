package com.devicehive.dao;

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


import com.devicehive.vo.DeviceClassEquipmentVO;
import com.devicehive.vo.DeviceClassWithEquipmentVO;

import javax.validation.constraints.NotNull;
import java.util.List;

public interface DeviceClassDao {

    void remove(long id);

    DeviceClassWithEquipmentVO find(long id);

    DeviceClassWithEquipmentVO persist(DeviceClassWithEquipmentVO deviceClass);

    DeviceClassWithEquipmentVO merge(DeviceClassWithEquipmentVO deviceClass);

    List<DeviceClassWithEquipmentVO> list(String name, String namePattern, String sortField,
                                                Boolean sortOrderAsc, Integer take, Integer skip);

    DeviceClassWithEquipmentVO findByName(@NotNull String name);

    DeviceClassEquipmentVO findDeviceClassEquipment(@NotNull long deviceClassId, @NotNull long equipmentId);
}
