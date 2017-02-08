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


import com.devicehive.vo.DeviceClassVO;

import javax.validation.constraints.NotNull;
import java.util.List;

public interface DeviceClassDao {

    void remove(long id);

    DeviceClassVO find(long id);

    DeviceClassVO persist(DeviceClassVO deviceClass);

    DeviceClassVO merge(DeviceClassVO deviceClass);

    List<DeviceClassVO> list(String name, String namePattern, String sortField,
                                                Boolean sortOrderAsc, Integer take, Integer skip);

    DeviceClassVO findByName(@NotNull String name);
}
