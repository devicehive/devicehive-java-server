package com.devicehive.client.api.client;


import com.devicehive.client.model.AccessKey;

import java.util.List;

public interface AccessKeyController {

    //keys block
    List<AccessKey> listKeys(long userId);

    AccessKey getKey(long userId, long keyId);

    AccessKey insertKey(long userId, AccessKey key);

    void updateKey(long userId, long keyId, AccessKey key);

    void deleteKey(long userId, long keyId);
}
