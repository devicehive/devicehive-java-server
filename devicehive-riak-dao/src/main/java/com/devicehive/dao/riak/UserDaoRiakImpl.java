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
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

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
        RiakUser riakUser = findBySecondaryIndex("login", name, USER_NS, RiakUser.class);
        RiakUser.convertToVo(riakUser);
        return Optional.ofNullable(RiakUser.convertToVo(riakUser));
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
    public List<UserVO> list(String login, String loginPattern,
            Integer role, Integer status,
            String sortField, boolean isSortOrderAsc,
            Integer take, Integer skip) {

        BucketMapReduce.Builder builder = new BucketMapReduce.Builder()
                .withNamespace(USER_NS);
        addMapValues(builder);
        if (login != null) {
            addReduceFilter(builder, "login", FilterOperator.EQUAL, login);
        } else if (loginPattern != null) {
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
        try {
            MapReduce.Response response = client.execute(builder.build());
            Collection<RiakUser> users = response.getResultsFromAllPhases(RiakUser.class);
            return users.stream().map(RiakUser::convertToVo).collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            throw new HivePersistenceLayerException("Cannot execute search user.", e);
        }
    }

}
