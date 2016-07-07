package com.devicehive.dao.rdbms;


import com.devicehive.dao.AccessKeyPermissionDao;
import com.devicehive.dao.CacheConfig;
import com.devicehive.model.AccessKey;

import java.util.Optional;

public class AccesskeyPermissionDaoImpl extends GenericDaoImpl implements AccessKeyPermissionDao {

    public int deleteByAccessKey(AccessKey key) {
        return createNamedQuery("AccessKeyPermission.deleteByAccessKey", Optional.<CacheConfig>empty())
                .setParameter("accessKey", key)
                .executeUpdate();
    }
}
