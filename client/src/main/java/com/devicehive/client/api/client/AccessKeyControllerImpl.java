package com.devicehive.client.api.client;


import com.devicehive.client.context.HiveContext;
import com.devicehive.client.model.AccessKey;
import com.google.common.reflect.TypeToken;

import javax.ws.rs.HttpMethod;
import java.util.List;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.*;

public class AccessKeyControllerImpl implements AccessKeyController {

    private final HiveContext hiveContext;

    public AccessKeyControllerImpl(HiveContext hiveContext) {
        this.hiveContext = hiveContext;
    }

    @Override
    public List<AccessKey> listKeys(long userId) {
        String path = "/user/" + userId + "/accesskey";
        return hiveContext.getHiveRestClient()
                .execute(path, HttpMethod.GET, null, null, new TypeToken<List<AccessKey>>() {
                }.getType(), ACCESS_KEY_LISTED);
    }

    @Override
    public AccessKey getKey(long userId, long keyId) {
        String path = "/user/" + userId + "/accesskey/" + keyId;
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, AccessKey.class, ACCESS_KEY_LISTED);
    }

    @Override
    public AccessKey insertKey(long userId, AccessKey key) {
        String path = "/user/" + userId + "/accesskey";
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, null, null, key, AccessKey.class,
                ACCESS_KEY_PUBLISHED, ACCESS_KEY_SUBMITTED);
    }

    @Override
    public void updateKey(long userId, long keyId, AccessKey key) {
        String path = "/user/" + userId + "/accesskey/" + keyId;
        hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, null, key, ACCESS_KEY_PUBLISHED);
    }

    @Override
    public void deleteKey(long userId, long keyId) {
        String path = "/user/" + userId + "/accesskey/" + keyId;
        hiveContext.getHiveRestClient().execute(path, HttpMethod.DELETE);
    }
}
