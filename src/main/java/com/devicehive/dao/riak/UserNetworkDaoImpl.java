package com.devicehive.dao.riak;


import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.indexes.IntIndexQuery;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.devicehive.dao.UserNetworkDao;
import com.devicehive.model.UserNetwork;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Profile({"riak"})
@Repository
public class UserNetworkDaoImpl implements UserNetworkDao {

    private static final Namespace USER_NETWORK_NS = new Namespace("userNetwork");

    @Autowired
    private RiakClient client;

    @Override
    public void persist(UserNetwork userNetwork) {
        try {
            String id = userNetwork.getUserId() + "n" + userNetwork.getNetworkId();
            userNetwork.setId(id);
            Location location = new Location(USER_NETWORK_NS, id);
            StoreValue storeOp = new StoreValue.Builder(userNetwork).withLocation(location).build();
            client.execute(storeOp);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public UserNetwork merge(UserNetwork existing) {
        try {
            Location location = new Location(USER_NETWORK_NS, existing.getId());
            StoreValue storeOp = new StoreValue.Builder(existing).withLocation(location).build();
            client.execute(storeOp);
            return existing;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<Long> findNetworksForUser(Long userId) {
        IntIndexQuery biq = new IntIndexQuery.Builder(USER_NETWORK_NS, "userId", userId).withKeyAndIndex(true).build();
        try {
            IntIndexQuery.Response response = client.execute(biq);
            List<IntIndexQuery.Response.Entry> entries = response.getEntries();
            if (entries.isEmpty()) {
                return null;
            }

            Set<Long> networks = new HashSet<>();
            for (IntIndexQuery.Response.Entry entry : entries) {
                Location location = entry.getRiakObjectLocation();
                FetchValue fetchOp = new FetchValue.Builder(location).build();
                UserNetwork un = client.execute(fetchOp).getValue(UserNetwork.class);
                networks.add(un.getNetworkId());
            }
            return networks;
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Set<Long> findUsersInNetwork(Long networkId) {
        IntIndexQuery biq = new IntIndexQuery.Builder(USER_NETWORK_NS, "networkId", networkId).build();
        try {
            IntIndexQuery.Response response = client.execute(biq);
            List<IntIndexQuery.Response.Entry> entries = response.getEntries();
            if (entries.isEmpty()) {
                return null;
            }

            Set<Long> users = new HashSet<>();
            for (IntIndexQuery.Response.Entry entry : entries) {
                Location location = entry.getRiakObjectLocation();
                FetchValue fetchOp = new FetchValue.Builder(location).build();
                UserNetwork un = client.execute(fetchOp).getValue(UserNetwork.class);
                users.add(un.getUserId());
            }
            return users;
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
