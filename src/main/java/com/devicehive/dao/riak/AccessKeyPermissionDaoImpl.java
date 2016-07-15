package com.devicehive.dao.riak;


import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.datatypes.CounterUpdate;
import com.basho.riak.client.api.commands.datatypes.FetchCounter;
import com.basho.riak.client.api.commands.datatypes.UpdateCounter;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.devicehive.dao.AccessKeyDao;
import com.devicehive.dao.AccessKeyPermissionDao;
import com.devicehive.dao.CacheConfig;
import com.devicehive.model.AccessKey;
import com.devicehive.model.AccessKeyPermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Profile({"riak"})
@Repository
public class AccessKeyPermissionDaoImpl implements AccessKeyPermissionDao {

    private static final Namespace COUNTER_NS = new Namespace("counters", "access_key_permission_counters");

    @Autowired
    private RiakClient client;

    @Autowired
    private AccessKeyDao accessKeyDao;

    @Override
    public int deleteByAccessKey(AccessKey key) {
        if (key.getPermissions() != null) {
            int result = key.getPermissions().size();
            key.getPermissions().clear();
            accessKeyDao.merge(key);
            return result;
        }
        return 0;
    }

    @Override
    public void persist(AccessKeyPermission accessKeyPermission) {
        merge(accessKeyPermission);
    }

    @Override
    public AccessKeyPermission merge(AccessKeyPermission accessKeyPermission) {
        AccessKey accessKey = accessKeyPermission.getAccessKey();
        accessKey = accessKeyDao.find(accessKey.getId());

        if (accessKeyPermission.getId() == null) {
            Location accessKeyPermissionCounters = new Location(COUNTER_NS, "access_key_permission_counter");
            CounterUpdate cu = new CounterUpdate(1);
            UpdateCounter update = new UpdateCounter.Builder(accessKeyPermissionCounters, cu).build();
            try {
                client.execute(update);
                FetchCounter fetch = new FetchCounter.Builder(accessKeyPermissionCounters).build();
                Long id = client.execute(fetch).getDatatype().view();
                accessKeyPermission.setId(id);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (accessKey.getPermissions() != null) {
            accessKey.getPermissions().add(accessKeyPermission);
        } else {
            Set<AccessKeyPermission> accessKeyPermissions = new HashSet<>();
            accessKeyPermissions.add(accessKeyPermission);
            accessKey.setPermissions(accessKeyPermissions);
        }

        accessKeyDao.merge(accessKey);

        return accessKeyPermission;
    }
}
