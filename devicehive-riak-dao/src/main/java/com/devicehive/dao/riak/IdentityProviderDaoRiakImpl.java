package com.devicehive.dao.riak;

/*
 * #%L
 * DeviceHive Dao Riak Implementation
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
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.devicehive.dao.IdentityProviderDao;
import com.devicehive.dao.riak.model.RiakIdentityProvider;
import com.devicehive.exceptions.HivePersistenceLayerException;
import com.devicehive.vo.IdentityProviderVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.concurrent.ExecutionException;

@Repository
public class IdentityProviderDaoRiakImpl extends RiakGenericDao implements IdentityProviderDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdentityProviderDaoRiakImpl.class);

    private static final Namespace CONFIG_NS = new Namespace("identity_provider");

    @Override
    public IdentityProviderVO getByName(@NotNull String name) {
        try {
            Location objectKey = new Location(CONFIG_NS, name);
            FetchValue fetch = new FetchValue.Builder(objectKey)
                    .withOption(quorum.getReadQuorumOption(), quorum.getReadQuorum())
                    .build();
            FetchValue.Response response = client.execute(fetch);
            RiakIdentityProvider object = getOrNull(response, RiakIdentityProvider.class);
            IdentityProviderVO entity = RiakIdentityProvider.convertToVO(object);
            return entity;
        } catch (ExecutionException | InterruptedException e) {
            LOGGER.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot fetch Identity by name.", e);
        }
    }

    @Override
    public boolean deleteById(@NotNull String name) {
        try {
            Location objectKey = new Location(CONFIG_NS, name);
            DeleteValue delete = new DeleteValue.Builder(objectKey).build();
            client.execute(delete);
        } catch (ExecutionException | InterruptedException e) {
            LOGGER.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot delete Identity by name.", e);
        }
        return true;
    }

    @Override
    public IdentityProviderVO merge(IdentityProviderVO existing) {
        try {
            RiakIdentityProvider riakIdentityProvider = RiakIdentityProvider.convertToEntity(existing);

            Location objectKey = new Location(CONFIG_NS, existing.getName());
            StoreValue storeOp = new StoreValue.Builder(riakIdentityProvider)
                    .withLocation(objectKey)
                    .withOption(quorum.getWriteQuorumOption(), quorum.getWriteQuorum())
                    .build();
            client.execute(storeOp);
        } catch (ExecutionException | InterruptedException e) {
            LOGGER.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot store Identity.", e);
        }
        return existing;
    }

    @Override
    public void persist(IdentityProviderVO identityProvider) {
        merge(identityProvider);
    }
}
