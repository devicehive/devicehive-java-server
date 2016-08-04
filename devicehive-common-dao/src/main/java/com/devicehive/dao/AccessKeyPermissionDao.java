package com.devicehive.dao;

import com.devicehive.model.AccessKey;
import com.devicehive.model.AccessKeyPermission;
import com.devicehive.vo.AccessKeyVO;

public interface AccessKeyPermissionDao {
    int deleteByAccessKey(AccessKeyVO key);

    void persist(AccessKeyVO key, AccessKeyPermission accessKeyPermission);

    AccessKeyPermission merge(AccessKeyPermission existing);
}
