package com.devicehive.client.impl;


import com.devicehive.client.AccessKeyController;
import com.devicehive.client.impl.context.HiveContext;
import com.devicehive.client.model.AccessKey;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.google.common.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.util.List;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.*;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

class AccessKeyControllerImpl implements AccessKeyController {

    private static final Logger logger = LoggerFactory.getLogger(AccessKeyControllerImpl.class);
    private final HiveContext hiveContext;

    public AccessKeyControllerImpl(HiveContext hiveContext) {
        this.hiveContext = hiveContext;
    }

    @Override
    public List<AccessKey> listKeys(long userId) {
        logger.debug("AccessKey: list requested for user id {}", userId);
        String path = "/user/" + userId + "/accesskey";
        List<AccessKey> result = hiveContext.getHiveRestClient()
                .execute(path, HttpMethod.GET, null, null, new TypeToken<List<AccessKey>>() {
                }.getType(), ACCESS_KEY_LISTED);
        logger.debug("AccessKey: list request for user id {} proceed successfully.", userId);
        return result;
    }

    @Override
    public List<AccessKey> listKeys() {
        logger.debug("AccessKey: list requested for current user");
        String path = "/user/current/accesskey";
        List<AccessKey> result = hiveContext.getHiveRestClient()
                .execute(path, HttpMethod.GET, null, null, new TypeToken<List<AccessKey>>() {
                }.getType(), ACCESS_KEY_LISTED);
        logger.debug("AccessKey: list request for current user proceed successfully.");
        return result;
    }

    @Override
    public AccessKey getKey(long userId, long keyId) {
        logger.debug("AccessKey: get requested for user with id {} and key id {}", userId, keyId);
        String path = "/user/" + userId + "/accesskey/" + keyId;
        AccessKey result = hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, AccessKey.class,
                ACCESS_KEY_LISTED);
        logger.debug("AccessKey: get request proceed successfully for user with id {} and key id {}", userId, keyId);
        return result;
    }

    @Override
    public AccessKey getKey(long keyId) {
        logger.debug("AccessKey: get requested for current user and key with id {}", keyId);
        String path = "/user/current/accesskey/" + keyId;
        AccessKey key = hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, AccessKey.class,
                ACCESS_KEY_LISTED);
        logger.debug("AccessKey: get request proceed successfully for current user and key with id {}", keyId);
        return key;
    }

    @Override
    public AccessKey insertKey(long userId, AccessKey key) throws HiveClientException {
        if (key == null) {
            throw new HiveClientException("key cannot be null!", BAD_REQUEST.getStatusCode());
        }
        logger.debug("AccessKey: insert requested for user with id {}. Key params: label {}, " +
                "expiration date {}", userId, key.getLabel(), key.getExpirationDate());
        String path = "/user/" + userId + "/accesskey";
        AccessKey result = hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, null, null, key,
                AccessKey.class, ACCESS_KEY_PUBLISHED, ACCESS_KEY_SUBMITTED);
        logger.debug("AccessKey: insert request proceed successfully for user with id {}. Key params: id {}, " +
                "label {}, expiration date {}", userId, key.getId(), key.getLabel(), key.getExpirationDate());
        return result;
    }

    @Override
    public AccessKey insertKey(AccessKey key) throws HiveClientException {
        if (key == null) {
            throw new HiveClientException("key cannot be null!", BAD_REQUEST.getStatusCode());
        }
        logger.debug("AccessKey: insert requested for current user. Key params: label {}, " +
                "expiration date {}", key.getLabel(), key.getExpirationDate());
        String path = "/user/current/accesskey";
        AccessKey result =
                hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, null, null, key, AccessKey.class,
                        ACCESS_KEY_PUBLISHED, ACCESS_KEY_SUBMITTED);
        logger.debug("AccessKey: insert request proceed successfully for current user. Key params: id {}, " +
                "label {}, expiration date {}", key.getId(), key.getLabel(), key.getExpirationDate());
        return result;
    }

    @Override
    public void updateKey(long userId, long keyId, AccessKey key) throws HiveClientException {
        if (key == null) {
            throw new HiveClientException("key cannot be null!", BAD_REQUEST.getStatusCode());
        }
        logger.debug("AccessKey: update requested for user with id {}. Key params: id {}, label {}, " +
                "expiration date {}", userId, keyId, key.getLabel(), key.getExpirationDate());
        String path = "/user/" + userId + "/accesskey/" + keyId;
        hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, null, key, ACCESS_KEY_PUBLISHED);
        logger.debug("AccessKey: update request proceed successfully for user with id {}. Key params: id {}, " +
                "label {}, expiration date {}", userId, keyId, key.getLabel(), key.getExpirationDate());
    }

    @Override
    public void updateKey(long keyId, AccessKey key) throws HiveClientException {
        if (key == null) {
            throw new HiveClientException("key cannot be null!", BAD_REQUEST.getStatusCode());
        }
        logger.debug("AccessKey: update requested for current user. Key params: id{}, label {}, " +
                "expiration date {}", keyId, key.getLabel(), key.getExpirationDate());
        String path = "/user/current/accesskey/" + keyId;
        hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, null, key, ACCESS_KEY_PUBLISHED);
        logger.debug("AccessKey: update request proceed successfully for current user. Key params: id {}, " +
                "label {}, expiration date {}", keyId, key.getLabel(), key.getExpirationDate());
    }

    @Override
    public void deleteKey(long userId, long keyId) {
        logger.debug("AccessKey: delete requested for user with id {}. Key id {}", userId, keyId);
        String path = "/user/" + userId + "/accesskey/" + keyId;
        hiveContext.getHiveRestClient().execute(path, HttpMethod.DELETE);
        logger.debug("AccessKey: delete request proceed successfully for user with id {}. Key id {}", userId, keyId);
    }

    @Override
    public void deleteKey(long keyId) {
        logger.debug("AccessKey: delete requested for current user. Key id {}", keyId);
        String path = "/user/current/accesskey/" + keyId;
        hiveContext.getHiveRestClient().execute(path, HttpMethod.DELETE);
        logger.debug("AccessKey: delete request proceed successfully for current user. Key id {}", keyId);
    }
}
