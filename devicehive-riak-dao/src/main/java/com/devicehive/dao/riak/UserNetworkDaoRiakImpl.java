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
import com.basho.riak.client.api.commands.indexes.IntIndexQuery;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.devicehive.exceptions.HivePersistenceLayerException;
import com.devicehive.dao.riak.model.UserNetwork;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Repository
public class UserNetworkDaoRiakImpl extends RiakGenericDao {

    private static final Namespace USER_NETWORK_NS = new Namespace("user_network");

    public void persist(UserNetwork userNetwork) {
        try {
            String id = userNetwork.getUserId() + "n" + userNetwork.getNetworkId();
            userNetwork.setId(id);
            Location location = new Location(USER_NETWORK_NS, id);
            StoreValue storeOp = new StoreValue.Builder(userNetwork)
                    .withLocation(location)
                    .withOption(quorum.getWriteQuorumOption(), quorum.getWriteQuorum())
                    .build();
            client.execute(storeOp);
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot persist user network.", e);
        }
    }

    public UserNetwork merge(UserNetwork existing) {
        try {
            Location location = new Location(USER_NETWORK_NS, existing.getId());
            StoreValue storeOp = new StoreValue.Builder(existing)
                    .withLocation(location)
                    .withOption(quorum.getWriteQuorumOption(), quorum.getWriteQuorum())
                    .build();
            client.execute(storeOp);
            return existing;
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot merge user network.", e);
        }
    }

    public void delete(long userId, long networkId) {
        String id = userId + "n" + networkId;
        Location location = new Location(USER_NETWORK_NS, id);
        DeleteValue deleteOp = new DeleteValue.Builder(location).build();
        try {
            client.execute(deleteOp);
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot delete network.", e);
        }
    }

    public Set<Long> findNetworksForUser(Long userId) {
        IntIndexQuery biq = new IntIndexQuery.Builder(USER_NETWORK_NS, "userId", userId).withKeyAndIndex(true).build();
        try {
            IntIndexQuery.Response response = client.execute(biq);
            List<UserNetwork> networkList = fetchMultiple(response, UserNetwork.class);
            Set<Long> networks = new HashSet<>();
            networkList.forEach(userNetwork -> networks.add(userNetwork.getNetworkId()));
            return networks;
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot find networks for user.", e);
        }
    }

    public Set<Long> findUsersInNetwork(Long networkId) {
        IntIndexQuery biq = new IntIndexQuery.Builder(USER_NETWORK_NS, "networkId", networkId).build();
        try {
            IntIndexQuery.Response response = client.execute(biq);
            List<UserNetwork> userNetworks = fetchMultiple(response, UserNetwork.class);
            Set<Long> users = new HashSet<>();
            userNetworks.forEach(userNetwork -> users.add(userNetwork.getUserId()));
            return users;
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot find users in network.", e);
        }
    }
}
