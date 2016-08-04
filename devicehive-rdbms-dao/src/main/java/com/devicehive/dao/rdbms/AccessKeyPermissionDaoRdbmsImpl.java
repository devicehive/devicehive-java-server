package com.devicehive.dao.rdbms;


import com.devicehive.dao.AccessKeyPermissionDao;
import com.devicehive.model.AccessKey;
import com.devicehive.model.AccessKeyPermission;
import com.devicehive.vo.AccessKeyVO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Profile({"rdbms"})
@Repository
public class AccessKeyPermissionDaoRdbmsImpl extends RdbmsGenericDao implements AccessKeyPermissionDao {

    @Override
    public int deleteByAccessKey(AccessKeyVO key) {
        AccessKey accessKey = AccessKey.convert(key);
        return createNamedQuery("AccessKeyPermission.deleteByAccessKey", Optional.<CacheConfig>empty())
                .setParameter("accessKey", accessKey)
                .executeUpdate();
    }

    @Override
    public void persist(AccessKeyVO key, AccessKeyPermission accessKeyPermission) {
        super.persist(accessKeyPermission);
    }

    @Override
    public AccessKeyPermission merge(AccessKeyPermission existing) {
        return super.merge(existing);
    }
}
