package com.devicehive.dao.riak;

import com.basho.riak.client.api.commands.indexes.BinIndexQuery;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.api.commands.mapreduce.BucketMapReduce;
import com.basho.riak.client.api.commands.mapreduce.MapReduce;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.dao.DeviceDao;
import com.devicehive.dao.NetworkDao;
import com.devicehive.dao.UserDao;
import com.devicehive.dao.riak.model.RiakNetwork;
import com.devicehive.dao.riak.model.UserNetwork;
import com.devicehive.exceptions.HivePersistenceLayerException;
import com.devicehive.vo.*;
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

    private static final Namespace NETWORK_NS = new Namespace("network");

    private static final Location COUNTERS_LOCATION = new Location(new Namespace("counters", "dh_counters"),
            "networkCounter");

    @Autowired
    private UserNetworkDaoRiakImpl userNetworkDao;

    @Autowired
    private NetworkDeviceDaoRiakImpl networkDeviceDao;

    private DeviceDao deviceDao;
    private UserDao userDao;

    public NetworkDaoRiakImpl() {
    }

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
            List<RiakNetwork> result = fetchMultiple(response, RiakNetwork.class);
            return result.stream().map(RiakNetwork::convert).collect(Collectors.toList());
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Can't find networks by name", e);
        }
    }

    @Override
    public void persist(@NotNull NetworkVO newNetwork) {
        if (newNetwork.getId() == null) {
            newNetwork.setId(getId(COUNTERS_LOCATION));
        }
        RiakNetwork network = RiakNetwork.convert(newNetwork);

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
        return RiakNetwork.convert(vo);
    }

    private RiakNetwork get(@NotNull Long networkId) {
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
            return RiakNetwork.convert(existing);
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Can't execute store operation on network network", e);
        }
    }

    @Override
    public void assignToNetwork(NetworkVO network, UserVO user) {
        assert network != null && network.getId() != null;
        assert user != null && user.getId() != null;

        Set<Long> networksForUser = userNetworkDao.findNetworksForUser(user.getId());
        if (!networksForUser.contains(network.getId())) {
            userNetworkDao.persist(new UserNetwork(user.getId(), network.getId()));
        }
    }

    @Override
    public List<NetworkVO> list(String name, String namePattern, String sortField, boolean isSortOrderAsc, Integer take,
            Integer skip, Optional<HivePrincipal> principalOptional) {
        BucketMapReduce.Builder builder = new BucketMapReduce.Builder()
                .withNamespace(NETWORK_NS);
        addMapValues(builder);

        if (name != null) {
            addReduceFilter(builder, "name", FilterOperator.EQUAL, name);
        } else if (namePattern != null) {
            namePattern = namePattern.replace("%", "");
            addReduceFilter(builder, "name", FilterOperator.REGEX, namePattern);
        }
        if (principalOptional.isPresent()) {
            HivePrincipal principal = principalOptional.get();
            if (principal != null && !principal.getRole().equals(HiveRoles.ADMIN)) {
                UserVO user = principal.getUser();
                if (user == null && principal.getKey() != null) {
                    user = principal.getKey().getUser();
                }

                if (user != null && !user.isAdmin()) {
                    Set<Long> networks = userNetworkDao.findNetworksForUser(user.getId());
                    addReduceFilter(builder, "id", FilterOperator.IN, networks);
                }

                if (principal.getKey() != null && principal.getKey().getPermissions() != null) {
                    Set<AccessKeyPermissionVO> permissions = principal.getKey().getPermissions();
                    Set<Long> ids = new HashSet<>();
                    permissions.stream().map((permission) -> permission.getNetworkIdsAsSet()).filter((id) -> (id != null)).forEach((id) -> {
                        ids.addAll(id);
                    });
                    if (!ids.isEmpty()) {
                        addReduceFilter(builder, "id", FilterOperator.IN, ids);
                    }
                } else if (principal.getDevice() != null) {
                    addReduceFilter(builder, "devices", FilterOperator.CONTAINS, principal.getDevice());
                }
            }
        }

        addReduceSort(builder, sortField, isSortOrderAsc);
        addReducePaging(builder, true, take, skip);
        try {
            MapReduce.Response response = client.execute(builder.build());

            return response.getResultsFromAllPhases(RiakNetwork.class).stream()
                    .map(RiakNetwork::convert).collect(Collectors.toList());
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
            Set<DeviceVO> devices = networkDeviceDao.findDevicesForNetwork(networkId).stream()
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
            Set<UserVO> users = userNetworkDao.findUsersInNetwork(networkId).stream()
                    .map(userDao::find)
                    .collect(Collectors.toSet());
            vo.setUsers(users);
            return Optional.of(vo);
        } else {
            return Optional.empty();
        }
    }
}
