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
import com.basho.riak.client.api.commands.indexes.BinIndexQuery;
import com.basho.riak.client.api.commands.indexes.IntIndexQuery;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.api.commands.mapreduce.BucketMapReduce;
import com.basho.riak.client.api.commands.mapreduce.MapReduce;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.devicehive.dao.AccessKeyDao;
import com.devicehive.dao.UserDao;
import com.devicehive.dao.riak.model.RiakAccessKey;
import com.devicehive.exceptions.HivePersistenceLayerException;
import com.devicehive.model.enums.AccessKeyType;
import com.devicehive.vo.AccessKeyPermissionVO;
import com.devicehive.vo.AccessKeyVO;
import com.devicehive.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Deprecated
@Repository
public class AccessKeyDaoRiakImpl extends RiakGenericDao implements AccessKeyDao {

    private static final Namespace ACCESS_KEY_NS = new Namespace("access_key");

    private static final Location COUNTERS_LOCATION = new Location(new Namespace("counters", "dh_counters"),
            "accessKeyCounter");

    @Autowired
    UserDao userDao;

    public AccessKeyDaoRiakImpl() {
    }

    @Override
    public AccessKeyVO getById(Long keyId, Long userId) {
        try {
            Location location = new Location(ACCESS_KEY_NS, String.valueOf(keyId));
            FetchValue fetchOp = new FetchValue.Builder(location)
                    .withOption(quorum.getReadQuorumOption(), quorum.getReadQuorum())
                    .build();
            FetchValue.Response execute = client.execute(fetchOp);
            RiakAccessKey result = getOrNull(execute, RiakAccessKey.class);
            if (result != null && userId != null && !result.getUser().getId().equals(userId)) {
                return null;
            }
            if (result != null) {
                return restoreReferences(result);
            } else {
                return null;
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot fetch access key by id and user id.", e);
        }
    }

    @Override
    public Optional<AccessKeyVO> getByKey(String key) {
        BinIndexQuery biq = new BinIndexQuery.Builder(ACCESS_KEY_NS, "key", key).build();
        try {
            BinIndexQuery.Response response = client.execute(biq);
            List<BinIndexQuery.Response.Entry> entries = response.getEntries();
            if (entries.isEmpty()) {
                return Optional.empty();
            } else {
                Location location = entries.get(0).getRiakObjectLocation();
                FetchValue fetchOp = new FetchValue.Builder(location)
                        .withOption(quorum.getReadQuorumOption(), quorum.getReadQuorum())
                        .build();
                FetchValue.Response execute = client.execute(fetchOp);
                RiakAccessKey result = getOrNull(execute, RiakAccessKey.class);
                AccessKeyVO vo = null;
                if (result != null) {
                    vo = restoreReferences(result);
                }
                return Optional.ofNullable(vo);
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot fetch access key by key string.", e);
        }
    }

    @Override
    public Optional<AccessKeyVO> getByUserAndLabel(UserVO user, String label) {
        IntIndexQuery biq = new IntIndexQuery.Builder(ACCESS_KEY_NS, "userId", user.getId()).build();
        try {
            IntIndexQuery.Response response = client.execute(biq);
            List<IntIndexQuery.Response.Entry> entries = response.getEntries();
            if (entries.isEmpty()) {
                return Optional.empty();
            }
            for (IntIndexQuery.Response.Entry e : entries) {
                Location location = e.getRiakObjectLocation();
                FetchValue fetchOp = new FetchValue.Builder(location)
                        .withOption(quorum.getReadQuorumOption(), quorum.getReadQuorum())
                        .build();
                FetchValue.Response execute = client.execute(fetchOp);
                RiakAccessKey accessKey = getOrNull(execute, RiakAccessKey.class);
                if (accessKey != null && accessKey.getLabel() != null && accessKey.getLabel().equals(label)) {
                    AccessKeyVO vo = restoreReferences(accessKey);
                    return Optional.of(vo);
                }
            }
            return Optional.empty();
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot fetch access key by user id and key label.", e);
        }
    }

    @Override
    public int deleteByIdAndUser(Long keyId, Long userId) {
        AccessKeyVO key = find(keyId);
        if (key != null && key.getUser() != null && !userId.equals(key.getUser().getId())) {
            return 0;
        }
        if (key != null) {
            try {
                Location location = new Location(ACCESS_KEY_NS, String.valueOf(key.getId()));
                DeleteValue delete = new DeleteValue.Builder(location).build();
                client.execute(delete);
                return 1;
            } catch (InterruptedException | ExecutionException e) {
                throw new HivePersistenceLayerException("Cannot delete access key by key id and user id.", e);
            }

        } else {
            return 0;
        }
    }

    @Override
    public int deleteById(Long keyId) {
        return deleteByIdAndUser(keyId, null);
    }

    @Override
    public int deleteOlderThan(Date date) {
        IntIndexQuery iiq = new IntIndexQuery.Builder(ACCESS_KEY_NS, "expirationDate", 0L, date.getTime()).build();
        try {
            IntIndexQuery.Response response = client.execute(iiq);
            List<IntIndexQuery.Response.Entry> entries = response.getEntries();
            if (entries.isEmpty()) {
                return 0;
            } else {
                for (IntIndexQuery.Response.Entry entry : entries) {
                    Location location = entry.getRiakObjectLocation();
                    deleteByIdAndUser(Long.parseLong(location.getKey().toString()), null);
                }
                return entries.size();
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot delete access key by key string.", e);
        }
    }

    @Override
    public AccessKeyVO find(Long id) {
        return getById(id, null);
    }

    @Override
    public void persist(AccessKeyVO accessKey) {
        merge(accessKey);
    }

    @Override
    public AccessKeyVO merge(AccessKeyVO key) {
        try {
            if (key.getId() == null) {
                key.setId(getId(COUNTERS_LOCATION));
            }

            Location accessKeyLocation = new Location(ACCESS_KEY_NS, String.valueOf(key.getId()));
            RiakAccessKey riakAccessKey = RiakAccessKey.convert(key);
            StoreValue storeOp = new StoreValue.Builder(riakAccessKey)
                    .withLocation(accessKeyLocation)
                    .withOption(quorum.getWriteQuorumOption(), quorum.getWriteQuorum())
                    .build();
            client.execute(storeOp);
            return restoreReferences(riakAccessKey);
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot store access key.", e);
        }
    }

    @Override
    public int deleteByAccessKey(AccessKeyVO key) {
        //TODO [rafa] that logic looks strange, i think in case of null we expected to clear whole collection.
        if (key.getPermissions() != null) {
            int result = key.getPermissions().size();
            key.getPermissions().clear();
            merge(key);
            return result;
        }
        return 0;
    }

    @Override
    public void persist(AccessKeyVO key, AccessKeyPermissionVO accessKeyPermission) {
        merge(key, accessKeyPermission);
    }

    @Override
    public AccessKeyPermissionVO merge(AccessKeyVO key, AccessKeyPermissionVO accessKeyPermission) {
        AccessKeyVO accessKeyVO = find(key.getId());
        if (accessKeyVO != null && accessKeyVO.getPermissions() != null) {
            //todo since permissions are stored inside the access key now, do we still need this cleanup?
            Iterator<AccessKeyPermissionVO> iterator = accessKeyVO.getPermissions().iterator();
            while (iterator.hasNext()) {
                AccessKeyPermissionVO next = iterator.next();
                if (accessKeyPermission.getId() != null && next.getId().equals(accessKeyPermission.getId())) {
                    iterator.remove();
                    break;
                }
            }

            accessKeyVO.getPermissions().add(accessKeyPermission);

            merge(accessKeyVO);
        }

        return accessKeyPermission;
    }

    @Override
    public List<AccessKeyVO> list(Long userId, String label,
            String labelPattern, Integer type,
            String sortField, Boolean isSortOrderAsc,
            Integer take, Integer skip) {
        BucketMapReduce.Builder builder = new BucketMapReduce.Builder()
                .withNamespace(ACCESS_KEY_NS);
        addMapValues(builder);
        addReduceFilter(builder, "userId", FilterOperator.EQUAL, userId);
        if (label != null) {
            addReduceFilter(builder, "label", FilterOperator.EQUAL, label);
        } else if (labelPattern != null) {
            labelPattern = labelPattern.replace("%", "");
            addReduceFilter(builder, "label", FilterOperator.REGEX, labelPattern);
        }
        if (type != null) {
            String typeString = AccessKeyType.getValueForIndex(type).toString();
            addReduceFilter(builder, "type", FilterOperator.EQUAL, typeString);
        }
        addReduceSort(builder, sortField, isSortOrderAsc);
        addReducePaging(builder, true, take, skip);
        try {
            MapReduce.Response response = client.execute(builder.build());
            return response.getResultsFromAllPhases(RiakAccessKey.class).stream()
                    .map(RiakAccessKey::convert).collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            throw new HivePersistenceLayerException("Cannot perform search access key.", e);
        }
    }

    private AccessKeyVO restoreReferences(RiakAccessKey key) {
        AccessKeyVO vo = RiakAccessKey.convert(key);

        if (vo.getUser() != null && vo.getUser().getId() != null) {
            UserVO user = userDao.find(vo.getUser().getId());
            vo.setUser(user);
        }

        return vo;
    }

}
