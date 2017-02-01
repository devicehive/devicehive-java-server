package com.devicehive.dao.rdbms;

/*
 * #%L
 * DeviceHive Dao RDBMS Implementation
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


import com.devicehive.dao.IdentityProviderDao;
import com.devicehive.model.IdentityProvider;
import com.devicehive.vo.IdentityProviderVO;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.Optional;

@Repository
public class IdentityProviderDaoRdbmsImpl extends RdbmsGenericDao implements IdentityProviderDao {
    @Override
    public IdentityProviderVO getByName(@NotNull String name) {
        IdentityProvider identityProvider = createNamedQuery(IdentityProvider.class, "IdentityProvider.getByName", Optional.of(CacheConfig.refresh()))
                .setParameter("name", name)
                .getResultList()
                .stream().findFirst().orElse(null);
        return IdentityProvider.convertToVO(identityProvider);
    }

    @Override
    public boolean deleteById(@NotNull String id) {
        return createNamedQuery("IdentityProvider.deleteByName", Optional.of(CacheConfig.bypass()))
                .setParameter("name", id)
                .executeUpdate() > 0;
    }

    @Override
    public IdentityProviderVO merge(IdentityProviderVO existing) {
        IdentityProvider identityProvider = IdentityProvider.convertToEntity(existing);
        IdentityProvider merge = super.merge(identityProvider);
        return IdentityProvider.convertToVO(merge);
    }

    @Override
    public void persist(IdentityProviderVO identityProvider) {
        IdentityProvider newEntity = IdentityProvider.convertToEntity(identityProvider);
        super.persist(newEntity);
    }
}
