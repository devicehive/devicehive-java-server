package com.devicehive.dao.riak;

import com.basho.riak.client.api.commands.indexes.BinIndexQuery;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.api.commands.mapreduce.BucketMapReduce;
import com.basho.riak.client.api.commands.mapreduce.MapReduce;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.devicehive.dao.DeviceDao;
import com.devicehive.dao.NetworkDao;
import com.devicehive.dao.UserDao;
import com.devicehive.dao.riak.model.RiakUser;
import com.devicehive.exceptions.HivePersistenceLayerException;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.UserVO;
import com.devicehive.vo.UserWithNetworkVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Profile({"riak"})
@Repository
public class UserDaoRiakImpl extends RiakGenericDao implements UserDao {

    private static final Namespace USER_NS = new Namespace("user");

    private static final Location COUNTERS_LOCATION = new Location(new Namespace("counters", "dh_counters"),
            "userCounter");

    @Autowired
    private UserNetworkDaoRiakImpl userNetworkDao;

    @Autowired
    private NetworkDao networkDao;

    @Autowired
    private NetworkDeviceDaoRiakImpl networkDeviceDao;

    @Autowired
    private DeviceDao deviceDao;

    public UserDaoRiakImpl() {
    }

    @PostConstruct
    public void init() {
        ((NetworkDaoRiakImpl) networkDao).setUserDao(this);
    }

    @Override
    public Optional<UserVO> findByName(String name) {
        UserVO user = findBySomeIdentityName(name, "login");
        return Optional.ofNullable(user);
    }

    @Override
    public UserVO findByGoogleName(String name) {
        return findBySomeIdentityName(name, "googleLogin");
    }

    @Override
    public UserVO findByFacebookName(String name) {
        return findBySomeIdentityName(name, "facebookLogin");
    }

    @Override
    public UserVO findByGithubName(String name) {
        return findBySomeIdentityName(name, "githubLogin");
    }

    @Override
    public Optional<UserVO> findByIdentityName(String login, String googleLogin, String facebookLogin, String githubLogin) {
        UserVO userToCheck;
        userToCheck = findByGoogleName(googleLogin);
        if (userToCheck != null) {
            if (doesUserAlreadyExist(userToCheck, login)) {
                return Optional.of(userToCheck);
            }
        }

        userToCheck = findByFacebookName(facebookLogin);
        if (userToCheck != null) {
            if (doesUserAlreadyExist(userToCheck, login)) {
                return Optional.of(userToCheck);
            }
        }

        userToCheck = findByGithubName(githubLogin);
        if (userToCheck != null) {
            if (doesUserAlreadyExist(userToCheck, login)) {
                return Optional.of(userToCheck);
            }
        }

        return Optional.empty();
    }

    @Override
    public long hasAccessToNetwork(UserVO user, NetworkVO network) {
        Set<Long> networks = userNetworkDao.findNetworksForUser(user.getId());
        if (networks != null && networks.contains(network.getId())) {
            return 1L;
        } else {
            return 0L;
        }
    }

    @Override
    public long hasAccessToDevice(UserVO user, String deviceGuid) {
        Set<Long> networkIds = userNetworkDao.findNetworksForUser(user.getId());
        for (Long networkId : networkIds) {
            Set<DeviceVO> devices = networkDeviceDao.findDevicesForNetwork(networkId).stream()
                    .map(deviceDao::findByUUID)
                    .collect(Collectors.toSet());
            if (devices != null) {
                long guidCount = devices
                        .stream()
                        .map(DeviceVO::getGuid)
                        .filter(g -> g.equals(deviceGuid))
                        .count();
                if (guidCount > 0) {
                    return guidCount;
                }
            }
        }
        return 0L;
    }

    @Override
    public UserWithNetworkVO getWithNetworksById(long id) {
        UserVO user = find(id);
        if (user == null) {
            return null;
        }

        Set<Long> networkIds = userNetworkDao.findNetworksForUser(id);
        UserWithNetworkVO userWithNetworkVO = UserWithNetworkVO.fromUserVO(user);
        if (networkIds != null) {
            //TODO [rafa] [implement bulk fetch method here]
            Set<NetworkVO> networks = new HashSet<>();
            for (Long networkId : networkIds) {
                NetworkVO network = networkDao.find(networkId);
                networks.add(network);
            }
            userWithNetworkVO.setNetworks(networks);
        }
        return userWithNetworkVO;
    }

    @Override
    public int deleteById(long id) {
        Location location = new Location(USER_NS, String.valueOf(id));
        DeleteValue deleteOp = new DeleteValue.Builder(location).build();
        try {
            client.execute(deleteOp);
            return 1;
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot delete by id", e);
        }
    }

    @Override
    public UserVO find(Long id) {
        try {
            Location location = new Location(USER_NS, String.valueOf(id));
            FetchValue fetchOp = new FetchValue.Builder(location)
                    .withOption(quorum.getReadQuorumOption(), quorum.getReadQuorum())
                    .build();
            RiakUser riakUser = getOrNull(client.execute(fetchOp), RiakUser.class);
            return RiakUser.convertToVo(riakUser);
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot find by id", e);
        }
    }

    @Override
    public void persist(UserVO user) {
        merge(user);
    }

    @Override
    public UserVO merge(UserVO user) {
        RiakUser entity = RiakUser.convertToEntity(user);
        try {
            if (entity.getId() == null) {
                entity.setId(getId(COUNTERS_LOCATION));
            }
            Location location = new Location(USER_NS, String.valueOf(entity.getId()));
            StoreValue storeOp = new StoreValue.Builder(entity)
                    .withLocation(location)
                    .withOption(quorum.getWriteQuorumOption(), quorum.getWriteQuorum())
                    .build();
            client.execute(storeOp);
            user.setId(entity.getId());
            return user;
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot merge user.", e);
        }
    }

    @Override
    public void unassignNetwork(@NotNull UserVO existingUser, @NotNull long networkId) {
        userNetworkDao.delete(existingUser.getId(), networkId);
    }

    @Override
    public List<UserVO> getList(String login, String loginPattern,
            Integer role, Integer status,
            String sortField, Boolean isSortOrderAsc,
            Integer take, Integer skip) {
        List<UserVO> result = new ArrayList<>();
        if (login != null) {
            Optional<UserVO> user = findByName(login);
            if (user.isPresent()) {
                result.add(user.get());
            }
        } else {
            try {
                BucketMapReduce.Builder builder = new BucketMapReduce.Builder()
                        .withNamespace(USER_NS);
                addMapValues(builder);
                if (loginPattern != null) {
                    loginPattern = loginPattern.replace("%", "");
                    addReduceFilter(builder, "login", FilterOperator.REGEX, loginPattern);
                }
                if (role != null) {
                    String roleString = UserRole.getValueForIndex(role).name();
                    addReduceFilter(builder, "role", FilterOperator.EQUAL, roleString);
                }
                if (status != null) {
                    String statusString = UserStatus.getValueForIndex(status).name();
                    addReduceFilter(builder, "status", FilterOperator.EQUAL, statusString);
                }

                addReduceSort(builder, sortField, isSortOrderAsc);
                addReducePaging(builder, true, take, skip);

                BucketMapReduce bmr = builder.build();
                MapReduce.Response response = client.execute(bmr);
                Collection<RiakUser> users = response.getResultsFromAllPhases(RiakUser.class);
                result.addAll(users.stream().map(RiakUser::convertToVo).collect(Collectors.toList()));
            } catch (InterruptedException | ExecutionException e) {
                throw new HivePersistenceLayerException("Cannot execute search user.", e);
            }
        }
        return result;
    }

    private boolean doesUserAlreadyExist(UserVO user, String login) {
        return (!user.getLogin().equals(login) && user.getStatus() != UserStatus.DELETED);
    }

    private UserVO findBySomeIdentityName(String name, String identityName) {
        if (name == null) {
            return null;
        }
        BinIndexQuery biq = new BinIndexQuery.Builder(USER_NS, identityName, name).build();
        try {
            BinIndexQuery.Response response = client.execute(biq);
            List<BinIndexQuery.Response.Entry> entries = response.getEntries();
            if (entries.isEmpty()) {
                return null;
            }
            Location location = entries.get(0).getRiakObjectLocation();
            FetchValue fetchOp = new FetchValue.Builder(location)
                    .withOption(quorum.getReadQuorumOption(), quorum.getReadQuorum())
                    .build();
            RiakUser riakUser = getOrNull(client.execute(fetchOp), RiakUser.class);
            return RiakUser.convertToVo(riakUser);
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot find by identity.", e);
        }
    }

}
