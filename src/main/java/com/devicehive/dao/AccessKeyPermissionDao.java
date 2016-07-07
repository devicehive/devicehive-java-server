package com.devicehive.dao;

import com.devicehive.model.AccessKey;

public interface AccessKeyPermissionDao {
    int deleteByAccessKey(AccessKey key);
}
