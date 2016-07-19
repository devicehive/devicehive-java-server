package com.devicehive.dao.riak;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.datatypes.CounterUpdate;
import com.basho.riak.client.api.commands.datatypes.FetchCounter;
import com.basho.riak.client.api.commands.datatypes.UpdateCounter;
import com.basho.riak.client.api.commands.indexes.BinIndexQuery;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.api.commands.mapreduce.BucketMapReduce;
import com.basho.riak.client.api.commands.mapreduce.MapReduce;
import com.basho.riak.client.api.commands.mapreduce.filters.SetMemberFilter;
import com.basho.riak.client.core.RiakFuture;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.basho.riak.client.core.query.functions.Function;
import com.basho.riak.client.core.util.BinaryValue;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.dao.DeviceDao;
import com.devicehive.dao.NetworkDao;
import com.devicehive.dao.UserDao;
import com.devicehive.dao.filter.AccessKeyBasedFilterForDevices;
import com.devicehive.exceptions.HivePersistenceLayerException;
import com.devicehive.model.Device;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Profile({"riak"})
@Repository
public class NetworkDaoImpl implements NetworkDao {

    private static final Namespace COUNTER_NS = new Namespace("counters", "network_counters");
    private static final Namespace NETWORK_NS = new Namespace("network");

    private Location networkCounter = new Location(COUNTER_NS, "network_counter");

    @Autowired
    private RiakClient client;

    @Autowired
    private UserNetworkDaoImpl userNetworkDao;

    @Autowired
    private NetworkDeviceDaoImpl networkDeviceDao;

    private DeviceDao deviceDao;
    private UserDao userDao;

    private final Map<String, String> sortMap = new HashMap<String, String>() {{
        put("name", "function(a,b){ return a.name %s b.name; }");
        put("description", "function(a,b){ return a.description %s b.description; }");
        put("entityVersion", "function(a,b){ return a.entityVersion %s b.entityVersion; }");
    }};

    void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    void setDeviceDao(DeviceDao deviceDao) {
        this.deviceDao = deviceDao;
    }

    @Override
    public List<Network> findByName(String name) {
        assert name != null;

        BinIndexQuery biq = new BinIndexQuery.Builder(NETWORK_NS, "name", name).build();
        try {
            BinIndexQuery.Response response = client.execute(biq);
            List<BinIndexQuery.Response.Entry> entries = response.getEntries();
            return entries.stream().map(entry -> {
                FetchValue fetchOp = new FetchValue.Builder(entry.getRiakObjectLocation()).build();
                try {
                    return client.execute(fetchOp).getValue(Network.class);
                } catch (ExecutionException | InterruptedException e) {
                    throw new HivePersistenceLayerException("Can't find networks by name", e);
                }
            }).collect(Collectors.toList());
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Can't find networks by name", e);
        }
    }

    @Override
    public void persist(@NotNull Network newNetwork) {
        try {
            if (newNetwork.getId() == null) {
                CounterUpdate cu = new CounterUpdate(1);
                UpdateCounter update = new UpdateCounter.Builder(networkCounter, cu).build();
                client.execute(update);

                FetchCounter fetchCounterOp = new FetchCounter.Builder(networkCounter).build();
                Long id = client.execute(fetchCounterOp).getDatatype().view();
                newNetwork.setId(id);
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Can't update or fetch next network counter value", e);
        }
        merge(newNetwork);
    }

    @Override
    public List<Network> getNetworksByIdsAndUsers(Long idForFiltering, Set<Long> networkdIds, Set<Long> permittedNetworks) {
        Set<Long> intersection = networkdIds;
        if (permittedNetworks != null) {
            intersection = networkdIds.stream()
                    .filter(permittedNetworks::contains)
                    .collect(Collectors.toSet());
        }
        Stream<Network> networkStream = intersection.stream()
                .map(this::findWithUsersAndDevices)
                .filter(Optional::isPresent)
                .map(Optional::get);
        if (idForFiltering != null) {
            networkStream = networkStream.filter(n -> n.getUsers().stream().anyMatch(u -> u.getId().equals(idForFiltering)));
        }
        return networkStream.collect(Collectors.toList());
    }

    @Override
    public int deleteById(long id) {
        Location location = new Location(NETWORK_NS, String.valueOf(id));
        DeleteValue deleteOp = new DeleteValue.Builder(location).build();
        try {
            client.execute(deleteOp);
            return 1;
        } catch (ExecutionException | InterruptedException e) {
            return 0;
        }
    }

    @Override
    public Network find(@NotNull Long networkId) {
        Location location = new Location(NETWORK_NS, String.valueOf(networkId));
        FetchValue fetchOp = new FetchValue.Builder(location).build();
        try {
            return client.execute(fetchOp).getValue(Network.class);
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Can't fetch network by id", e);
        }
    }

    @Override
    public Network merge(@NotNull Network network) {
        assert network.getId() != null;

        Location location = new Location(NETWORK_NS, String.valueOf(network.getId()));
        StoreValue storeOp = new StoreValue.Builder(network).withLocation(location).build();
        try {
            client.execute(storeOp);
            return network;
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Can't execute store operation on network network", e);
        }
    }

    @Override
    public List<Network> list(String name, String namePattern, String sortField, boolean sortOrderAsc, Integer take,
                              Integer skip, Optional<HivePrincipal> principal) {
        String sortFunc = sortMap.get(sortField);
        if (sortFunc == null) {
            sortFunc = sortMap.get("name");
        }

        BucketMapReduce.Builder builder = new BucketMapReduce.Builder()
                .withNamespace(NETWORK_NS)
                .withMapPhase(Function.newNamedJsFunction("Riak.mapValuesJson"));

        if (name != null) {
            String func = String.format(
                    "function(values, arg) {" +
                            "return values.filter(function(v) {" +
                            "var name = v.name;" +
                            "return name == '%s';" +
                            "})" +
                            "}", name);
            Function function = Function.newAnonymousJsFunction(func);
            builder.withReducePhase(function);
        } else if (namePattern != null) {
            namePattern = namePattern.replace("%", "");
            String func = String.format(
                    "function(values, arg) {" +
                            "return values.filter(function(v) {" +
                            "var name = v.name;" +
                            "return name.indexOf('%s') > -1;" +
                            "})" +
                            "}", namePattern);
            Function function = Function.newAnonymousJsFunction(func);
            builder.withReducePhase(function);
        }

        principal.flatMap(p -> {
            User user = p.getUser();
            if (user == null && p.getKey().getUser() != null) {
                user = p.getKey().getUser();
            }

            Set<Long> networkIds = null;
            if (user != null && !user.isAdmin()) {
                networkIds = userNetworkDao.findNetworksForUser(user.getId());
            }
            if (p.getKey() != null && p.getKey().getPermissions() != null && (user == null || !user.isAdmin())) {
                networkIds = AccessKeyBasedFilterForDevices.createExtraFilters(p.getKey().getPermissions()).stream()
                        .filter(f -> f.getNetworkIds() != null)
                        .flatMap(f -> f.getNetworkIds().stream())
                        .collect(Collectors.toSet());
            }
            return Optional.ofNullable(networkIds);
        }).ifPresent(networkIds -> {
            List<String> ids = networkIds.stream().map(Object::toString).collect(Collectors.toList());

            builder.withKeyFilter(new SetMemberFilter<>(ids.toArray(new String[ids.size()])));
        });

        builder.withReducePhase(Function.newNamedJsFunction("Riak.reduceSort"),
                String.format(sortFunc, sortOrderAsc ? ">" : "<"),
                take == null);

        if (take != null) {
            int[] args = new int[2];
            args[0] = skip != null ? skip : 0;
            args[1] = args[0] + take;
            builder.withReducePhase(Function.newNamedJsFunction("Riak.reduceSlice"), args, true);
        }

        BucketMapReduce bmr = builder.build();
        RiakFuture<MapReduce.Response, BinaryValue> future = client.executeAsync(bmr);
        try {
            MapReduce.Response response = future.get();
            return response.getResultsFromAllPhases(Network.class).stream().collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Network> findFirstByName(String name) {
        return findByName(name).stream().findFirst();
    }

    private Optional<Network> findWithUsersAndDevices(long networkId) {
        return Optional.ofNullable(find(networkId))
                .map(network -> {
                    Set<User> users = userNetworkDao.findUsersInNetwork(networkId).stream()
                            .map(userDao::find)
                            .collect(Collectors.toSet());
                    network.setUsers(users);
                    Set<Device> devices = networkDeviceDao.findDevicesForNetwork(networkId).stream()
                            .map(deviceDao::findByUUID)
                            .collect(Collectors.toSet());
                    network.setDevices(devices);
                    return network;
                });
    }

    @Override
    public Optional<Network> findWithUsers(long networkId) {
        return Optional.ofNullable(find(networkId))
                .map(network -> {
                    Set<User> users = userNetworkDao.findUsersInNetwork(networkId).stream()
                            .map(userDao::find)
                            .collect(Collectors.toSet());
                    network.setUsers(users);
                    return network;
                });
    }
}
