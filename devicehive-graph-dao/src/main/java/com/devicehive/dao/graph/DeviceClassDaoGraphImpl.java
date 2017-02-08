package com.devicehive.dao.graph;

/*
 * #%L
 * DeviceHive Dao RDBMS Implementation
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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

import com.devicehive.dao.DeviceClassDao;
import com.devicehive.vo.DeviceClassVO;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.List;

@Repository
public class DeviceClassDaoGraphImpl implements DeviceClassDao {

    @Override
    public void remove(long id) {

    }

    @Override
    public DeviceClassVO find(long id) {
        return null;
    }

    @Override
    public DeviceClassVO persist(DeviceClassVO deviceClass) {
        return null;
    }

    @Override
    public DeviceClassVO merge(DeviceClassVO deviceClass) {
        return null;
    }

    @Override
    public List<DeviceClassVO> list(String name, String namePattern, String sortField, Boolean sortOrderAsc, Integer take, Integer skip) {
        return null;
    }

    @Override
    public DeviceClassVO findByName(@NotNull String name) {
        return null;
    }
}
