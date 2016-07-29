package com.devicehive.dao.riak;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.indexes.BinIndexQuery;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.api.commands.mapreduce.BucketMapReduce;
import com.basho.riak.client.api.commands.mapreduce.MapReduce;
import com.basho.riak.client.core.RiakFuture;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.basho.riak.client.core.query.functions.Function;
import com.basho.riak.client.core.util.BinaryValue;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.Constants;
import com.devicehive.dao.DeviceDao;
import com.devicehive.dao.NetworkDao;
import com.devicehive.dao.UserDao;
import com.devicehive.dao.riak.model.RiakNetwork;
import com.devicehive.exceptions.HivePersistenceLayerException;
import com.devicehive.model.AccessKeyPermission;
import com.devicehive.model.Device;
import com.devicehive.vo.NetworkVO;
import com.devicehive.model.User;
import com.devicehive.model.UserNetwork;
import com.devicehive.vo.NetworkWithUsersAndDevicesVO;
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

    @Autowired
    private RiakQuorum quorum;

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
    public List<NetworkVO> findByName(String name) {
        if (name == null) {
            return Collections.emptyList();
        }

        BinIndexQuery biq = new BinIndexQuery.Builder(NETWORK_NS, "name", name).build();
        try {
            BinIndexQuery.Response response = client.execute(biq);
            return fetchMultiple(response, NetworkVO.class);
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Can't find networks by name", e);
        }
    }

    @Override
    public void persist(@NotNull NetworkVO newNetwork) {
        if (newNetwork.getId() == null) {
            newNetwork.setId(getId(networkCounter));
        }
        RiakNetwork network = new RiakNetwork(newNetwork);

        Location location = new Location(NETWORK_NS, String.valueOf(network.getId()));
        StoreValue storeOp = new StoreValue.Builder(network)
                .withLocation(location)
                .withOption(quorum.getWriteQuorumOption(), quorum.getWriteQuorum())
                .build();
        try {
            client.execute(storeOp);
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Can't execute store operation on network network", e);
        }
    }

    @Override
    public List<NetworkWithUsersAndDevicesVO> getNetworksByIdsAndUsers(Long idForFiltering, Set<Long> networkdIds, Set<Long> permittedNetworks) {
        Set<Long> intersection = networkdIds;
        if (permittedNetworks != null) {
            intersection = networkdIds.stream()
                    .filter(permittedNetworks::contains)
                    .collect(Collectors.toSet());
        }
        Stream<NetworkWithUsersAndDevicesVO> networkStream = intersection.stream()
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
    public NetworkVO find(@NotNull Long networkId) {
        RiakNetwork vo = get(networkId);
        return vo != null ? vo.convert() : null;
    }

    private RiakNetwork get(@NotNull  Long networkId) {
        Location location = new Location(NETWORK_NS, String.valueOf(networkId));
        FetchValue fetchOp = new FetchValue.Builder(location)
                .withOption(quorum.getReadQuorumOption(), quorum.getReadQuorum())
                .build();
        try {
            return getOrNull(client.execute(fetchOp), RiakNetwork.class);
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Can't fetch network by id", e);
        }
    }

    @Override
    public NetworkVO merge(@NotNull NetworkVO network) {
        assert network.getId() != null;

        RiakNetwork existing = get(network.getId());
        existing.setKey(network.getKey());
        existing.setName(network.getName());
        existing.setDescription(network.getDescription());

        Location location = new Location(NETWORK_NS, String.valueOf(network.getId()));
        StoreValue storeOp = new StoreValue.Builder(existing)
                .withLocation(location)
                .withOption(quorum.getWriteQuorumOption(), quorum.getWriteQuorum())
                .build();
        try {
            client.execute(storeOp);
            return existing.convert();
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Can't execute store operation on network network", e);
        }
    }

    @Override
    public void assignToNetwork(NetworkVO network, User user) {
        assert network != null && network.getId() != null;
        assert user != null && user.getId() != null;

        Set<Long> networksForUser = userNetworkDao.findNetworksForUser(user.getId());
        if (!networksForUser.contains(network.getId())) {
            userNetworkDao.persist(new UserNetwork(user.getId(), network.getId()));
        }
    }

    @Override
    public List<NetworkVO> list(String name, String namePattern, String sortField, boolean sortOrderAsc, Integer take,
                              Integer skip, Optional<HivePrincipal> principalOptional) {
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

        if (principalOptional.isPresent()) {
            HivePrincipal principal = principalOptional.get();
            if (principal != null && !principal.getRole().equals(HiveRoles.ADMIN)) {
                User user = principal.getUser();
                if (user == null && principal.getKey() != null) {
                    user = principal.getKey().getUser();
                }

                if (user != null && !user.isAdmin()) {
                    Set<Long> networks = userNetworkDao.findNetworksForUser(user.getId());
                    String functionString =
                            "function(values, arg) {" +
                                    "return values.filter(function(v) {" +
                                    "var networkId = v.id;" +
                                    "return arg.indexOf(networkId) > -1;" +
                                    "})" +
                                    "}";
                    Function reduceFunction = Function.newAnonymousJsFunction(functionString);
                    builder.withReducePhase(reduceFunction, networks);
                }

                if (principal.getKey() != null && principal.getKey().getPermissions() != null) {
                    Set<AccessKeyPermission> permissions = principal.getKey().getPermissions();
                    Set<Long> ids = new HashSet<>();
                    for (AccessKeyPermission permission : permissions) {
                        Set<Long> id = permission.getNetworkIdsAsSet();
                        if (id != null) {
                            ids.addAll(id);
                        }
                    }

                    String functionString =
                            "function(values, arg) {" +
                                    "return values.filter(function(v) {" +
                                    "return arg.indexOf(v.id) > -1;" +
                                    "})" +
                                    "}";
                    Function reduceFunction = Function.newAnonymousJsFunction(functionString);
                    if (!ids.isEmpty()) builder.withReducePhase(reduceFunction, ids);
                } else if (principal.getDevice() != null) {
                    String functionString =
                            "function(values, arg) {" +
                                    "return values.filter(function(v) {" +
                                    "var devices = v.devices;" +
                                    "if (devices == null) return false;" +
                                    "return devices.indexOf(arg) > -1;" +
                                    "})" +
                                    "}";
                    Function reduceFunction = Function.newAnonymousJsFunction(functionString);
                    builder.withReducePhase(reduceFunction, principal.getDevice());
                }
            }
        }

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
            return response.getResultsFromAllPhases(NetworkVO.class).stream()
                    .skip(skip)
                    .limit(take)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            throw new HivePersistenceLayerException("Cannot get list of networks.", e);
        }
    }

    @Override
    public Optional<NetworkVO> findFirstByName(String name) {
        return findByName(name).stream().findFirst();
    }

    private Optional<NetworkWithUsersAndDevicesVO> findWithUsersAndDevices(long networkId) {
        Optional<NetworkWithUsersAndDevicesVO> result = findWithUsers(networkId);

        if (result.isPresent()) {
            Set<Device> devices = networkDeviceDao.findDevicesForNetwork(networkId).stream()
                    .map(deviceDao::findByUUID)
                    .collect(Collectors.toSet());
            result.get().setDevices(devices);
            return result;
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<NetworkWithUsersAndDevicesVO> findWithUsers(long networkId) {
        NetworkVO networkVO = find(networkId);
        if (networkVO != null) {
            NetworkWithUsersAndDevicesVO vo = new NetworkWithUsersAndDevicesVO(networkVO);
            Set<User> users = userNetworkDao.findUsersInNetwork(networkId).stream()
                    .map(userDao::find)
                    .collect(Collectors.toSet());
            vo.setUsers(users);
            return Optional.of(vo);
        } else {
            return Optional.empty();
        }
    }
}
