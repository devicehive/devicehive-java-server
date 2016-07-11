package com.devicehive.dao.riak;


import com.devicehive.dao.AccessKeyPermissionDao;
import com.devicehive.dao.CacheConfig;
import com.devicehive.model.AccessKey;
import com.devicehive.model.AccessKeyPermission;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Profile({"riak"})
@Repository
public class AccesskeyPermissionDaoImpl extends GenericDaoImpl implements AccessKeyPermissionDao {

    @Override
    public int deleteByAccessKey(AccessKey key) {
        return createNamedQuery("AccessKeyPermission.deleteByAccessKey", Optional.<CacheConfig>empty())
                .setParameter("accessKey", key)
                .executeUpdate();
    }

    @Override
    public AccessKeyPermission find(Long id) {
        return find(AccessKeyPermission.class, id);
    }

    @Override
    public void persist(AccessKeyPermission accessKeyPermission) {
        super.persist(accessKeyPermission);
    }

    @Override
    public AccessKeyPermission merge(AccessKeyPermission existing) {
        return super.merge(existing);
    }
}
