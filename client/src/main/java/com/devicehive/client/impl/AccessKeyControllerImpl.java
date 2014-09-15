package com.devicehive.client.impl;


import com.google.common.reflect.TypeToken;

import com.devicehive.client.AccessKeyController;
import com.devicehive.client.impl.context.RestAgent;
import com.devicehive.client.model.AccessKey;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.devicehive.client.model.exceptions.HiveException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.List;

import javax.ws.rs.HttpMethod;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.ACCESS_KEY_LISTED;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.ACCESS_KEY_PUBLISHED;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.ACCESS_KEY_SUBMITTED;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

class AccessKeyControllerImpl implements AccessKeyController {

    private static final Logger logger = LoggerFactory.getLogger(AccessKeyControllerImpl.class);
    private final RestAgent restAgent;

    AccessKeyControllerImpl(RestAgent restAgent) {
        this.restAgent = restAgent;
    }

    @Override
    public List<AccessKey> listKeys(long userId) throws HiveException {
        logger.debug("AccessKey: list requested for user id {}", userId);
        String path = "/user/" + userId + "/accesskey";
        Type type = new TypeToken<List<AccessKey>>() {
            private static final long serialVersionUID = 5031432595147474481L;
        }.getType();
        List<AccessKey> result = restAgent
            .execute(path, HttpMethod.GET, null, null, type, ACCESS_KEY_LISTED);
        logger.debug("AccessKey: list request for user id {} proceed successfully.", userId);
        return result;
    }

    @Override
    public List<AccessKey> listKeys() throws HiveException {
        logger.debug("AccessKey: list requested for current user");
        String path = "/user/current/accesskey";
        Type type = new TypeToken<List<AccessKey>>() {
            private static final long serialVersionUID = 6171432595147474481L;
        }.getType();
        List<AccessKey> result = restAgent
            .execute(path, HttpMethod.GET, null, null, type, ACCESS_KEY_LISTED);
        logger.debug("AccessKey: list request for current user proceed successfully.");
        return result;
    }

    @Override
    public AccessKey getKey(long userId, long keyId) throws HiveException {
        logger.debug("AccessKey: get requested for user with id {} and key id {}", userId, keyId);
        String path = "/user/" + userId + "/accesskey/" + keyId;
        AccessKey result = restAgent.execute(path, HttpMethod.GET, null, AccessKey.class, ACCESS_KEY_LISTED);
        logger.debug("AccessKey: get request proceed successfully for user with id {} and key id {}", userId, keyId);
        return result;
    }

    @Override
    public AccessKey getKey(long keyId) throws HiveException {
        logger.debug("AccessKey: get requested for current user and key with id {}", keyId);
        String path = "/user/current/accesskey/" + keyId;
        AccessKey key = restAgent.execute(path, HttpMethod.GET, null,
                                          AccessKey.class,
                                          ACCESS_KEY_LISTED);
        logger.debug("AccessKey: get request proceed successfully for current user and key with id {}", keyId);
        return key;
    }

    @Override
    public AccessKey insertKey(long userId, AccessKey key) throws HiveException {
        if (key == null) {
            throw new HiveClientException("key cannot be null!", BAD_REQUEST.getStatusCode());
        }
        logger.debug("AccessKey: insert requested for user with id {}. Key params: label {}, " +
                     "expiration date {}", userId, key.getLabel(), key.getExpirationDate());
        String path = "/user/" + userId + "/accesskey";
        AccessKey result = restAgent
            .execute(path, HttpMethod.POST, null, null, key, AccessKey.class, ACCESS_KEY_PUBLISHED,
                     ACCESS_KEY_SUBMITTED);
        logger.debug("AccessKey: insert request proceed successfully for user with id {}. Key params: id {}, " +
                     "label {}, expiration date {}", userId, key.getId(), key.getLabel(), key.getExpirationDate());
        return result;
    }

    @Override
    public AccessKey insertKey(AccessKey key) throws HiveException {
        if (key == null) {
            throw new HiveClientException("key cannot be null!", BAD_REQUEST.getStatusCode());
        }
        logger.debug("AccessKey: insert requested for current user. Key params: label {}, " +
                     "expiration date {}", key.getLabel(), key.getExpirationDate());
        String path = "/user/current/accesskey";
        AccessKey result =
            restAgent.execute(path, HttpMethod.POST, null, null, key,
                              AccessKey.class, ACCESS_KEY_PUBLISHED, ACCESS_KEY_SUBMITTED);
        logger.debug("AccessKey: insert request proceed successfully for current user. Key params: id {}, " +
                     "label {}, expiration date {}", key.getId(), key.getLabel(), key.getExpirationDate());
        return result;
    }

    @Override
    public void updateKey(long userId, AccessKey key) throws HiveException {
        if (key == null || key.getId() == null) {
            throw new HiveClientException("key cannot be null!", BAD_REQUEST.getStatusCode());
        }
        logger.debug("AccessKey: update requested for user with id {}. Key params: id {}, label {}, " +
                     "expiration date {}", userId, key.getId(), key.getLabel(), key.getExpirationDate());
        String path = "/user/" + userId + "/accesskey/" + key.getId();
        restAgent.execute(path, HttpMethod.PUT, null, key, ACCESS_KEY_PUBLISHED);
        logger.debug("AccessKey: update request proceed successfully for user with id {}. Key params: id {}, " +
                     "label {}, expiration date {}", userId, key.getId(), key.getLabel(), key.getExpirationDate());
    }

    @Override
    public void updateKey(AccessKey key) throws HiveException {
        if (key == null || key.getId() == null) {
            throw new HiveClientException("key cannot be null!", BAD_REQUEST.getStatusCode());
        }
        logger.debug("AccessKey: update requested for current user. Key params: id{}, label {}, " +
                     "expiration date {}", key.getId(), key.getLabel(), key.getExpirationDate());
        String path = "/user/current/accesskey/" + key.getId();
        restAgent.execute(path, HttpMethod.PUT, null, key, ACCESS_KEY_PUBLISHED);
        logger.debug("AccessKey: update request proceed successfully for current user. Key params: id {}, " +
                     "label {}, expiration date {}", key.getId(), key.getLabel(), key.getExpirationDate());
    }

    @Override
    public void deleteKey(long userId, long keyId) throws HiveException {
        logger.debug("AccessKey: delete requested for user with id {}. Key id {}", userId, keyId);
        String path = "/user/" + userId + "/accesskey/" + keyId;
        restAgent.execute(path, HttpMethod.DELETE);
        logger.debug("AccessKey: delete request proceed successfully for user with id {}. Key id {}", userId, keyId);
    }

    @Override
    public void deleteKey(long keyId) throws HiveException {
        logger.debug("AccessKey: delete requested for current user. Key id {}", keyId);
        String path = "/user/current/accesskey/" + keyId;
        restAgent.execute(path, HttpMethod.DELETE);
        logger.debug("AccessKey: delete request proceed successfully for current user. Key id {}", keyId);
    }
}
