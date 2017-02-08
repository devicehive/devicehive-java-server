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

import com.devicehive.dao.IdentityProviderDao;
import com.devicehive.vo.IdentityProviderVO;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;

@Repository
public class IdentityProviderDaoGraphImpl implements IdentityProviderDao {

    @Override
    public IdentityProviderVO getByName(@NotNull String name) {
        return null;
    }

    @Override
    public boolean deleteById(@NotNull String name) {
        return false;
    }

    @Override
    public IdentityProviderVO merge(IdentityProviderVO existing) {
        return null;
    }

    @Override
    public void persist(IdentityProviderVO identityProvider) {

    }
}
