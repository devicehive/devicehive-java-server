package com.devicehive.dao;

import com.devicehive.model.AccessKey;
import com.devicehive.model.AccessKeyPermission;

public interface AccessKeyPermissionDao {
    int deleteByAccessKey(AccessKey key);

    void persist(AccessKeyPermission accessKeyPermission);

    AccessKeyPermission merge(AccessKeyPermission existing);
}
