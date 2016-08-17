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


import com.devicehive.vo.DeviceEquipmentVO;
import com.devicehive.vo.DeviceVO;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Created by Gleb on 07.07.2016.
 */
public interface DeviceEquipmentDao {

    List<DeviceEquipmentVO> getByDevice(@NotNull DeviceVO device);

    DeviceEquipmentVO getByDeviceAndCode(@NotNull String code, @NotNull DeviceVO device);

    DeviceEquipmentVO merge(DeviceEquipmentVO deviceEquipment, DeviceVO device);

    void persist(DeviceEquipmentVO deviceEquipment, DeviceVO device);
}
