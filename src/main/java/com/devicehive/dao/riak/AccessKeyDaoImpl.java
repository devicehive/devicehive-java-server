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
import com.basho.riak.client.core.RiakFuture;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.basho.riak.client.core.query.functions.Function;
import com.basho.riak.client.core.util.BinaryValue;
import com.devicehive.dao.AccessKeyDao;
import com.devicehive.dao.UserDao;
import com.devicehive.exceptions.HivePersistenceLayerException;
import com.devicehive.model.AccessKey;
import com.devicehive.model.AccessKeyPermission;
import com.devicehive.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ExecutionException;


@Profile({"riak"})
@Repository
public class AccessKeyDaoImpl extends RiakGenericDao implements AccessKeyDao {

    private static final Namespace ACCESS_KEY_NS = new Namespace("accessKey");
    private static final Namespace USER_AND_LABEL_ACCESS_KEY_NS = new Namespace("userAndLabelAccessKey");
    private static final Location COUNTERS_LOCATION = new Location(new Namespace("counters", "check_counters"),
            "accessKeyCounter");

    @Autowired
    RiakClient client;

    @Autowired
    UserDao userDao;

    private final Map<String, String> sortMap = new HashMap<>();

    public AccessKeyDaoImpl() {
        sortMap.put("label", "function(a,b){ return a.label %s b.label; }");
        sortMap.put("expirationDate", "function(a,b){ return a.expirationDate %s b.expirationDate; }");
        sortMap.put("type", "function(a,b){ return a.type %s b.type; }");
        sortMap.put("entityVersion", "function(a,b){ return a.entityVersion %s b.entityVersion; }");
    }


    @Override
    public AccessKey getById(Long keyId, Long userId) {
        try {
            Location location = new Location(ACCESS_KEY_NS, String.valueOf(keyId));
            FetchValue fetchOp = new FetchValue.Builder(location).build();
            FetchValue.Response execute = client.execute(fetchOp);
            AccessKey result = getOrNull(execute, AccessKey.class);
            if (result != null) {
                User user = userDao.find(result.getUserId());
                return restoreReferences(result, user);
            } else {
                return null;
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot fetch access key by id and user id.", e);
        }
    }

    @Override
    public Optional<AccessKey> getByKey(String key) {
        BinIndexQuery biq = new BinIndexQuery.Builder(ACCESS_KEY_NS, "key", key).build();
        try {
            BinIndexQuery.Response response = client.execute(biq);
            List<BinIndexQuery.Response.Entry> entries = response.getEntries();
            if (entries.isEmpty()) {
                return null;
            } else {
                Location location = entries.get(0).getRiakObjectLocation();
                FetchValue fetchOp = new FetchValue.Builder(location).build();
                FetchValue.Response execute = client.execute(fetchOp);
                AccessKey result = getOrNull(execute, AccessKey.class);
                if (result != null) {
                    //todo: discuss - may be it's better to keep user inside
                    User user = userDao.find(result.getUserId());
                    restoreReferences(result, user);
                }
                return Optional.ofNullable(result);
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot fetch access key by key string.", e);
        }
    }

    @Override
    public Optional<AccessKey> getByUserAndLabel(User user, String label) {
        Location location = new Location(USER_AND_LABEL_ACCESS_KEY_NS, String.valueOf(user.getId()) + "n" + label);
        FetchValue fetchOp = new FetchValue.Builder(location).build();
        try {
            FetchValue.Response execute = client.execute(fetchOp);
            Long accessKeyId = getOrNull(execute, Long.class);
            if (accessKeyId != null) {
                AccessKey value = find(accessKeyId);
                return Optional.ofNullable(value);
            }
            return Optional.empty();
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot fetch access key by user id and key label.", e);
        }
    }

    @Override
    public int deleteByIdAndUser(Long keyId, Long userId) {
        AccessKey key = find(keyId);
        if (userId == null) {
            userId = key.getUserId();
        } else if (key != null && userId != key.getUserId()) {
            return 0;
        }
        if (key != null) {
            String label = key.getLabel();
            try {
                Location location = new Location(USER_AND_LABEL_ACCESS_KEY_NS, String.valueOf(userId) + "n" + label);
                DeleteValue delete = new DeleteValue.Builder(location).build();
                client.execute(delete);

                location = new Location(ACCESS_KEY_NS, String.valueOf(key));
                delete = new DeleteValue.Builder(location).build();
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
    public AccessKey find(Long id) {
        return getById(id, null);
    }

    @Override
    public void persist(AccessKey accessKey) {
        merge(accessKey);
    }

    @Override
    public AccessKey merge(AccessKey key) {
        try {
            long userId = key.getUserId();
            User user = key.getUser();
            Location location = new Location(USER_AND_LABEL_ACCESS_KEY_NS, String.valueOf(userId) + "n" + key.getLabel());
            if (key.getId() == null) {
                key.setId(getId(COUNTERS_LOCATION));
                StoreValue storeValue = new StoreValue.Builder(key.getId()).withLocation(location).build();
                client.execute(storeValue);
            } else {
                AccessKey existing = find(key.getId());
                if (existing != null && existing.getLabel().equals(key.getLabel())) {
                    DeleteValue delete = new DeleteValue.Builder(location).build();
                    client.execute(delete);
                }
                StoreValue storeValue = new StoreValue.Builder(key.getId()).withLocation(location).build();
                client.execute(storeValue);

            }
            Location accessKeyLocation = new Location(ACCESS_KEY_NS, String.valueOf(key.getId()));
            removeReferences(key);
            StoreValue storeOp = new StoreValue.Builder(key).withLocation(accessKeyLocation).build();
            client.execute(storeOp);
            return restoreReferences(key, user);
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot store access key.", e);
        }
    }

    @Override
    public List<AccessKey> list(Long userId, String label,
                                String labelPattern, Integer type,
                                String sortField, Boolean sortOrderAsc,
                                Integer take, Integer skip) {
        List<AccessKey> result = new ArrayList<>();

        try {
            String sortFunction = sortMap.get(sortField);
            if (sortFunction == null) {
                sortFunction = sortMap.get("label");
            }
            BucketMapReduce.Builder builder = new BucketMapReduce.Builder()
                    .withNamespace(ACCESS_KEY_NS)
                    .withMapPhase(Function.newNamedJsFunction("Riak.mapValuesJson"))
                    .withReducePhase(Function.newNamedJsFunction("Riak.reduceSort"),
                            String.format(sortFunction, sortOrderAsc ? ">" : "<"),
                            take == null && labelPattern == null && label == null && type == null);
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
                                "    return v.label == %s;" +
                                "  })" +
                                "}", label);
                builder = builder.withReducePhase(Function.newAnonymousJsFunction(functionBody), take == null && type == null);
            }
            if (type != null) {
                String functionBody = String.format(
                        "function(values, arg) {" +
                                "  return values.filter(function(v) {" +
                                "    return v.type == %s;" +
                                "  })" +
                                "}", type);
                builder = builder.withReducePhase(Function.newAnonymousJsFunction(functionBody), take == null);
            }

            builder = addPaging(builder, take, skip);
            BucketMapReduce bmr = builder.build();
            RiakFuture<MapReduce.Response, BinaryValue> future = client.executeAsync(bmr);
            future.await();
            MapReduce.Response response = future.get();
            result.addAll(response.getResultsFromAllPhases(AccessKey.class));
        } catch (InterruptedException | ExecutionException e) {
            throw new HivePersistenceLayerException("Cannot perform search access key.", e);
        }

        return result;
    }

    private void removeReferences(AccessKey key) {
        key.setUser(null);
        if (key.getPermissions() != null) {
            for (AccessKeyPermission permission : key.getPermissions()) {
                permission.setAccessKey(null);
            }
        }
    }

    private AccessKey restoreReferences(AccessKey key, User user) {
        key.setUser(user);
        if (key.getPermissions() != null) {
            for (AccessKeyPermission permission : key.getPermissions()) {
                permission.setAccessKey(key);
            }
        }
        return key;
    }

}
