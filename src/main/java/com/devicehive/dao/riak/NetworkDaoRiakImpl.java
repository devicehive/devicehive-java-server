package com.devicehive.dao.riak;

import com.basho.riak.client.api.RiakClient;
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
import com.devicehive.configuration.Constants;
import com.devicehive.dao.DeviceDao;
import com.devicehive.dao.NetworkDao;
import com.devicehive.dao.UserDao;
import com.devicehive.dao.filter.AccessKeyBasedFilterForDevices;
import com.devicehive.exceptions.HivePersistenceLayerException;
import com.devicehive.model.Device;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import com.devicehive.model.UserNetwork;
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
public class NetworkDaoRiakImpl extends RiakGenericDao implements NetworkDao {

    private static final Namespace COUNTER_NS = new Namespace("counters", "network_counters");
    private static final Namespace NETWORK_NS = new Namespace("network");

    private Location networkCounter = new Location(COUNTER_NS, "network_counter");

    @Autowired
    private RiakClient client;

    @Autowired
    private UserNetworkDaoRiakImpl userNetworkDao;

    @Autowired
    private NetworkDeviceDaoRiakImpl networkDeviceDao;

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
            return fetchMultiple(response, Network.class);
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Can't find networks by name", e);
        }
    }

    @Override
    public void persist(@NotNull Network newNetwork) {
        if (newNetwork.getId() == null) {
            newNetwork.setId(getId(networkCounter));
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
            return getOrNull(client.execute(fetchOp), Network.class);
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
    public void assignToNetwork(Network network, User user) {
        assert network != null && network.getId() != null;
        assert user != null && user.getId() != null;

        Set<Long> networksForUser = userNetworkDao.findNetworksForUser(user.getId());
        if (!networksForUser.contains(network.getId())) {
            userNetworkDao.persist(new UserNetwork(user.getId(), network.getId()));
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
                .withMapPhase(Function.newAnonymousJsFunction("function(riakObject, keyData, arg) { " +
                        "                if(riakObject.values[0].metadata['X-Riak-Deleted']){ return []; } " +
                        "                else { return Riak.mapValuesJson(riakObject, keyData, arg); }}"))
                .withReducePhase(Function.newAnonymousJsFunction("function(values, arg) {" +
                        "return values.filter(function(v) {" +
                        "if (v === [] || v.name === null) { return false; }" +
                        "return true;" +
                        "})" +
                        "}"));

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
                            "if (name === null) { return false; }" +
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
            } else if (p.getKey() != null && p.getKey().getPermissions() != null && (user == null || !user.isAdmin())) {
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
                true);

        if (take == null)
            take = Constants.DEFAULT_TAKE;
        if (skip == null)
            skip = 0;

        BucketMapReduce bmr = builder.build();
        RiakFuture<MapReduce.Response, BinaryValue> future = client.executeAsync(bmr);
        try {
            MapReduce.Response response = future.get();
            return response.getResultsFromAllPhases(Network.class).stream()
                    .skip(skip)
                    .limit(take)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            throw new HivePersistenceLayerException("Cannot get list of networks.", e);
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
