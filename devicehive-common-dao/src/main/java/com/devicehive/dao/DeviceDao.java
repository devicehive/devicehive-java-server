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


import com.devicehive.auth.HivePrincipal;
import com.devicehive.vo.DeviceVO;

import java.util.List;

/**
 * Created by Gleb on 07.07.2016.
 */
public interface DeviceDao {

    DeviceVO findById(String id);

    void persist(DeviceVO device);

    DeviceVO merge(DeviceVO device);

    int deleteById(String id);

    List<DeviceVO> getDeviceList(List<String> ids, HivePrincipal principal);

    List<DeviceVO> list(String name, String namePattern, Long networkId, String networkName,
                         String sortField, boolean sortOrderAsc, Integer take, Integer skip, HivePrincipal principal);

    long count(String name, String namePattern, Long networkId, String networkName, HivePrincipal principal);
}
