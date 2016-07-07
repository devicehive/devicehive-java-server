package com.devicehive.dao.rdbms;

import com.devicehive.dao.AccessKeyDao;
import com.devicehive.dao.CacheConfig;
import com.devicehive.model.AccessKey;
import com.devicehive.model.User;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public class AccessKeyDaoImpl extends GenericDaoImpl implements AccessKeyDao {

    public AccessKey getById(Long keyId, Long userId) {
        return createNamedQuery(AccessKey.class, "AccessKey.getById", Optional.of(CacheConfig.refresh()))
                .setParameter("userId", userId)
                .setParameter("accessKeyId", keyId)
                .getResultList()
                .stream().findFirst().orElse(null);
    }

    public Optional<AccessKey> getByKey(String key) {
        return createNamedQuery(AccessKey.class, "AccessKey.getByKey", Optional.of(CacheConfig.get()))
                .setParameter("someKey", key)
                .getResultList().stream().findFirst();
    }

    public Optional<AccessKey> getByUserAndLabel(User user, String label) {
        return createNamedQuery(AccessKey.class, "AccessKey.getByUserAndLabel", Optional.<CacheConfig>empty())
                .setParameter("userId", user.getId())
                .setParameter("label", label)
                .getResultList().stream().findFirst();
    }

    public int deleteByIdAndUser(Long keyId, Long userId) {
        return createNamedQuery("AccessKey.deleteByIdAndUser", Optional.<CacheConfig>empty())
                .setParameter("accessKeyId", keyId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    @Override
    public int deleteById(Long keyId) {
        return createNamedQuery("AccessKey.deleteById", Optional.<CacheConfig>empty())
                .setParameter("accessKeyId", keyId)
                .executeUpdate();
    }

    @Override
    public int deleteOlderThan(Date date) {
        return createNamedQuery("AccessKey.deleteOlderThan", Optional.<CacheConfig>empty())
                .setParameter("expirationDate", date)
                .executeUpdate();
    }

}
