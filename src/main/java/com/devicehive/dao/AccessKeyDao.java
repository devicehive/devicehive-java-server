package com.devicehive.dao;

import com.devicehive.model.AccessKey;
import com.devicehive.model.User;

import java.util.Date;
import java.util.Optional;

public interface AccessKeyDao {
    AccessKey getById(Long keyId, Long userId);

    Optional<AccessKey> getByKey(String key);

    Optional<AccessKey> getByUserAndLabel(User user, String label);

    int deleteByIdAndUser(Long keyId, Long userId);

    int deleteById(Long keyId);

    int deleteOlderThan(Date date);

    AccessKey find(Long id);

    void persist(AccessKey accessKey);

    AccessKey merge(AccessKey existing);
}
