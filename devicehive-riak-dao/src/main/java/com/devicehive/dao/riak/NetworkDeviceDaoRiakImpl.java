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

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.indexes.BinIndexQuery;
import com.basho.riak.client.api.commands.indexes.IntIndexQuery;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.devicehive.exceptions.HivePersistenceLayerException;
import com.devicehive.dao.riak.model.NetworkDevice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Profile({"riak"})
@Repository
public class NetworkDeviceDaoRiakImpl extends RiakGenericDao {

    private static final Namespace NETWORK_DEVICE_NS = new Namespace("network_device");

    @Autowired
    private RiakClient client;

    @Autowired
    private RiakQuorum quorum;

    public void saveOrUpdate(NetworkDevice networkDevice) {
        try {
            Set<Long> networks = findNetworksForDevice(networkDevice.getDeviceUuid());
            if (!networks.contains(networkDevice.getNetworkId())) {
                String id = networkDevice.getNetworkId() + "n" + networkDevice.getDeviceUuid();
                networkDevice.setId(id);
                Location location = new Location(NETWORK_DEVICE_NS, id);
                StoreValue store = new StoreValue.Builder(networkDevice)
                        .withLocation(location)
                        .withOption(quorum.getWriteQuorumOption(), quorum.getWriteQuorum())
                        .build();
                client.execute(store);
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Can't store networkDevice relation", e);
        }
    }

    public void delete(long networkId, String deviceUuid) {
        String id = networkId + "n" + deviceUuid;
        Location location = new Location(NETWORK_DEVICE_NS, id);
        DeleteValue delete = new DeleteValue.Builder(location).build();
        try {
            client.execute(delete);
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Can't delete networkDevice relation", e);
        }
    }

    public Set<String> findDevicesForNetwork(long networkId) {
        IntIndexQuery biq = new IntIndexQuery.Builder(NETWORK_DEVICE_NS, "networkId", networkId).build();
        try {
            IntIndexQuery.Response response = client.execute(biq);

            List<NetworkDevice> ndList = fetchMultiple(response, NetworkDevice.class);
            Set<String> devices = new HashSet<>();
            ndList.forEach(networkDevice -> devices.add(networkDevice.getDeviceUuid()));
            return devices;
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot find device for network.", e);
        }
    }

    public Set<Long> findNetworksForDevice(String deviceUuid) {
        BinIndexQuery biq = new BinIndexQuery.Builder(NETWORK_DEVICE_NS, "deviceUuid", deviceUuid).build();
        try {
            BinIndexQuery.Response response = client.execute(biq);
            List<NetworkDevice> nds = fetchMultiple(response, NetworkDevice.class);
            Set<Long> networks = new HashSet<>();
            nds.forEach(networkDevice ->  networks.add(networkDevice.getNetworkId()));
            return networks;
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot find networks for device.", e);
        }
    }
}
