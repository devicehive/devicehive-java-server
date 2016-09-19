package com.devicehive.dao.riak;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.indexes.BinIndexQuery;
import com.basho.riak.client.api.commands.indexes.IntIndexQuery;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.api.commands.mapreduce.BucketMapReduce;
import com.basho.riak.client.api.commands.mapreduce.MapReduce;
import com.basho.riak.client.api.commands.mapreduce.filters.SetMemberFilter;
import com.basho.riak.client.api.convert.ConverterFactory;
import com.basho.riak.client.core.RiakFuture;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.basho.riak.client.core.query.functions.Function;
import com.basho.riak.client.core.util.BinaryValue;
import com.devicehive.application.RiakQuorum;
import com.devicehive.configuration.Constants;
import com.devicehive.dao.riak.converters.GsonConverter;
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

@Repository
public class AccessKeyDaoRiakImpl extends RiakGenericDao implements AccessKeyDao {

    private static final Namespace ACCESS_KEY_NS = new Namespace("access_key");

    private static final Location COUNTERS_LOCATION = new Location(new Namespace("counters", "dh_counters"),
            "accessKeyCounter");

    @Autowired
    RiakClient client;

    @Autowired
    UserDao userDao;

    @Autowired
    RiakQuorum quorum;

    private final Map<String, String> sortMap = new HashMap<>();

    public AccessKeyDaoRiakImpl() {
        sortMap.put("label", "function(a,b){ return a.label %s b.label; }");
        sortMap.put("expirationDate", "function(a,b){ return a.expirationDate %s b.expirationDate; }");
        sortMap.put("type", "function(a,b){ return a.type %s b.type; }");
        sortMap.put("entityVersion", "function(a,b){ return a.entityVersion %s b.entityVersion; }");
        //ConverterFactory.getInstance().registerConverterForClass(RiakAccessKey.class, new GsonConverter<>(RiakAccessKey.class));
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
                                  String sortField, Boolean sortOrderAsc,
                                  Integer take, Integer skip) {
        try {
            String sortFunction = sortMap.get(sortField);
            if (sortFunction == null) {
                sortFunction = sortMap.get("label");
            }
            if (sortOrderAsc == null) {
                sortOrderAsc = true;
            }
            BucketMapReduce.Builder builder = new BucketMapReduce.Builder()
                    .withNamespace(ACCESS_KEY_NS)
                    .withMapPhase(Function.newAnonymousJsFunction("function(riakObject, keyData, arg) { " +
                            "                if(riakObject.values[0].metadata['X-Riak-Deleted']){ return []; } " +
                            "                else { return Riak.mapValuesJson(riakObject, keyData, arg); }}"))
                    .withReducePhase(Function.newAnonymousJsFunction("function(values, arg) {" +
                            "return values.filter(function(v) {" +
                            "if (v === []) { return false; }" +
                            "return true;" +
                            "})" +
                            "}"));

            if (userId != null) {
                IntIndexQuery iiq = new IntIndexQuery.Builder(ACCESS_KEY_NS, "userId", userId).build();

                IntIndexQuery.Response response = client.execute(iiq);
                List<IntIndexQuery.Response.Entry> entries = response.getEntries();
                Set<String> keys = new HashSet<>();
                if (entries.isEmpty()) {
                    return Collections.emptyList();
                } else {
                    for (IntIndexQuery.Response.Entry entry : entries) {
                        Location location = entry.getRiakObjectLocation();
                        keys.add(location.getKeyAsString());
                    }
                }
                builder = builder.withKeyFilter(new SetMemberFilter<>(keys));
            }
            if (labelPattern != null) {
                String functionBody = String.format(
                        "function(values, arg) {" +
                                "  return values.filter(function(v) {" +
                                "    if (v.label === null) { return false; }" +
                                "    return v.label.indexOf('%s') > -1;" +
                                "  })" +
                                "}", labelPattern);
                builder = builder.withReducePhase(Function.newAnonymousJsFunction(functionBody),
                        take == null && label == null && type == null);
            }
            if (label != null) {
                String functionBody = String.format(
                        "function(values, arg) {" +
                                "  return values.filter(function(v) {" +
                                "    return v.label == '%s';" +
                                "  })" +
                                "}", label);
                builder = builder.withReducePhase(Function.newAnonymousJsFunction(functionBody), take == null && type == null);
            }
            if (type != null) {
                String typeString = AccessKeyType.getValueForIndex(type).toString();
                String functionBody = String.format(
                        "function(values, arg) {" +
                                "  return values.filter(function(v) {" +
                                "    return v.type == '%s';" +
                                "  })" +
                                "}", typeString);
                builder = builder.withReducePhase(Function.newAnonymousJsFunction(functionBody), take == null);
            }

            builder.withReducePhase(Function.newNamedJsFunction("Riak.reduceSort"),
                    String.format(sortFunction, sortOrderAsc ? ">" : "<"),
                    true);

            if (take == null)
                take = Constants.DEFAULT_TAKE;
            if (skip == null)
                skip = 0;

            BucketMapReduce bmr = builder.build();
            RiakFuture<MapReduce.Response, BinaryValue> future = client.executeAsync(bmr);
            future.await();
            MapReduce.Response response = future.get();
            List<RiakAccessKey> keys = response.getResultsFromAllPhases(RiakAccessKey.class).stream()
                    .skip(skip)
                    .limit(take)
                    .collect(Collectors.toList());
            return keys.stream().map(RiakAccessKey::convert).collect(Collectors.toList());
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
