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

import com.devicehive.dao.ConfigurationDao;
import com.devicehive.vo.ConfigurationVO;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ConfigurationDaoGraphImpl implements ConfigurationDao {

    @Override
    public Optional<ConfigurationVO> getByName(String name) {
        return null;
    }

    @Override
    public int delete(String name) {
        return 0;
    }

    @Override
    public void persist(ConfigurationVO configuration) {

    }

    @Override
    public ConfigurationVO merge(ConfigurationVO existing) {
        return null;
    }
}
