package com.devicehive.dao.riak;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.indexes.BinIndexQuery;
import com.basho.riak.client.api.commands.indexes.IntIndexQuery;
import com.basho.riak.client.api.commands.indexes.SecondaryIndexQuery;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.devicehive.exceptions.HivePersistenceLayerException;
import com.devicehive.model.NetworkDevice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Profile({"riak"})
@Repository
public class NetworkDeviceDaoImpl {

    private static final Namespace NETWORK_DEVICE_NS = new Namespace("networkDevice");

    @Autowired
    private RiakClient client;

    public void persist(NetworkDevice networkDevice) {
        try {
            String id = networkDevice.getNetworkId() + "n" + networkDevice.getDeviceUuid();
            networkDevice.setId(id);
            Location location = new Location(NETWORK_DEVICE_NS, id);
            StoreValue store = new StoreValue.Builder(networkDevice).withLocation(location).build();
            client.execute(store);
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
            List<FetchValue> fetchRequests = response.getEntries().stream()
                    .map(SecondaryIndexQuery.Response.Entry::getRiakObjectLocation)
                    .map(loc -> new FetchValue.Builder(loc).build())
                    .collect(Collectors.toList());
            Set<String> devices = new HashSet<>();
            for (FetchValue fetch : fetchRequests) {
                NetworkDevice nd = client.execute(fetch).getValue(NetworkDevice.class);
                devices.add(nd.getDeviceUuid());
            }
            return devices;
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return Collections.emptySet();
        }
    }

    public Set<Long> findNetworksForDevice(String deviceUuid) {
        BinIndexQuery biq = new BinIndexQuery.Builder(NETWORK_DEVICE_NS, "deviceUuid", deviceUuid).build();
        try {
            BinIndexQuery.Response response = client.execute(biq);
            List<FetchValue> fetchRequests = response.getEntries().stream()
                    .map(SecondaryIndexQuery.Response.Entry::getRiakObjectLocation)
                    .map(loc -> new FetchValue.Builder(loc).build())
                    .collect(Collectors.toList());
            Set<Long> networks = new HashSet<>();
            for (FetchValue fetch : fetchRequests) {
                NetworkDevice nd = client.execute(fetch).getValue(NetworkDevice.class);
                networks.add(nd.getNetworkId());
            }
            return networks;
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return Collections.emptySet();
        }
    }
}
