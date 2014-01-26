package com.devicehive.client.impl;


import com.devicehive.client.OAuthGrantController;
import com.devicehive.client.impl.context.HiveContext;
import com.devicehive.client.model.AccessType;
import com.devicehive.client.model.OAuthGrant;
import com.devicehive.client.model.OAuthType;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.google.common.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.*;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

class OAuthGrantControllerImpl implements OAuthGrantController {

    private static final Logger logger = LoggerFactory.getLogger(OAuthGrantControllerImpl.class);
    private final HiveContext hiveContext;

    public OAuthGrantControllerImpl(HiveContext hiveContext) {
        this.hiveContext = hiveContext;
    }

    @Override
    public List<OAuthGrant> list(long userId, Timestamp start, Timestamp end, String clientOauthId, OAuthType type,
                                 String scope, String redirectUri, AccessType accessType, String sortField,
                                 String sortOrder, Integer take, Integer skip) {
        logger.debug("OAuthGrant: list requested with parameters: userId {}, start timestamp {], end timestamp {}, " +
                "client OAuth identifier {}, OAuth grant type {}, OAuth scope {} OAuth redirect URI {}, " +
                "access type {}, sort field {}, sort order {}, take {}, skip {}", userId, start, end, clientOauthId,
                type, scope, redirectUri, accessType, sortField, sortOrder, take, skip);
        String path = "/user/" + userId + "/oauth/grant";
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("start", start);
        queryParams.put("end", end);
        queryParams.put("clientOAuthId", clientOauthId);
        queryParams.put("type", type);
        queryParams.put("scope", scope);
        queryParams.put("redirectUri", redirectUri);
        queryParams.put("accessType", accessType);
        queryParams.put("sortField", sortField);
        queryParams.put("sortOrder", sortOrder);
        queryParams.put("take", take);
        queryParams.put("skip", skip);
        List<OAuthGrant> result = hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, queryParams,
                new TypeToken<List<OAuthGrant>>() {
                }.getType(), OAUTH_GRANT_LISTED);
        logger.debug(
                "OAuthGrant: list request proceed for parameters: userId {}, start timestamp {], end timestamp {}, " +
                        "client OAuth identifier {}, OAuth grant type {}, OAuth scope {} OAuth redirect URI {}, " +
                        "access type {}, sort field {}, sort order {}, take {}, skip {}", userId, start, end,
                clientOauthId, type, scope, redirectUri, accessType, sortField, sortOrder, take, skip);
        return result;
    }

    @Override
    public List<OAuthGrant> list(Timestamp start, Timestamp end, String clientOauthId, OAuthType type, String scope,
                                 String redirectUri, AccessType accessType, String sortField, String sortOrder,
                                 Integer take, Integer skip) {
        logger.debug("OAuthGrant: list requested for current user with parameters: start timestamp {], " +
                "end timestamp {}, " +
                "client OAuth identifier {}, OAuth grant type {}, OAuth scope {} OAuth redirect URI {}, " +
                "access type {}, sort field {}, sort order {}, take {}, skip {}", start, end, clientOauthId,
                type, scope, redirectUri, accessType, sortField, sortOrder, take, skip);
        String path = "/user/current/oauth/grant";
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("start", start);
        queryParams.put("end", end);
        queryParams.put("clientOAuthId", clientOauthId);
        queryParams.put("type", type);
        queryParams.put("scope", scope);
        queryParams.put("redirectUri", redirectUri);
        queryParams.put("accessType", accessType);
        queryParams.put("sortField", sortField);
        queryParams.put("sortOrder", sortOrder);
        queryParams.put("take", take);
        queryParams.put("skip", skip);
        List<OAuthGrant> result = hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, queryParams,
                new TypeToken<List<OAuthGrant>>() {
                }.getType(), OAUTH_GRANT_LISTED);
        logger.debug("OAuthGrant: list proceed for current user with parameters: start timestamp {], " +
                "end timestamp {}, " +
                "client OAuth identifier {}, OAuth grant type {}, OAuth scope {} OAuth redirect URI {}, " +
                "access type {}, sort field {}, sort order {}, take {}, skip {}", start, end, clientOauthId,
                type, scope, redirectUri, accessType, sortField, sortOrder, take, skip);
        return result;
    }

    @Override
    public OAuthGrant get(long userId, long grantId) {
        logger.debug("OAuthGrant: get requested for user id {} and grant id {}", userId, grantId);
        String path = "/user/" + userId + "/oauth/grant/" + grantId;
        OAuthGrant result = hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, OAuthGrant.class,
                OAUTH_GRANT_LISTED);
        logger.debug("OAuthGrant: get proceed for user id {} and grant id {}", userId, grantId);
        return result;
    }

    @Override
    public OAuthGrant get(long grantId) {
        logger.debug("OAuthGrant: get requested for current user and grant id {}", grantId);
        String path = "/user/current/oauth/grant/" + grantId;
        OAuthGrant result = hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, OAuthGrant.class,
                OAUTH_GRANT_LISTED);
        logger.debug("OAuthGrant: get proceed for current user and grant id {}", grantId);
        return result;
    }

    @Override
    public OAuthGrant insert(long userId, OAuthGrant grant) throws HiveClientException {
        if (grant == null) {
            throw new HiveClientException("OAuthGrant cannot be null!", BAD_REQUEST.getStatusCode());
        }
        logger.debug("OAuthGrant: insert requested for user with id {} and grant with scope {} and type {}", userId,
                grant.getScope(), grant.getType());
        String path = "/user/" + userId + "/oauth/grant";
        OAuthGrant result;
        if (OAuthType.TOKEN.equals(grant.getType())) {
            result = hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, null, null, grant,
                    OAuthGrant.class,
                    OAUTH_GRANT_PUBLISHED, OAUTH_GRANT_SUBMITTED_TOKEN);
        } else {
            result = hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, null, null, grant,
                    OAuthGrant.class,
                    OAUTH_GRANT_PUBLISHED, OAUTH_GRANT_SUBMITTED_CODE);
        }
        logger.debug("OAuthGrant: insert proceed for user with id {} and grant with scope {} and type {}.Result id " +
                "{}", userId, grant.getScope(), grant.getType(), result.getId());
        return result;
    }

    @Override
    public OAuthGrant insert(OAuthGrant grant) throws HiveClientException {
        if (grant == null) {
            throw new HiveClientException("OAuthGrant cannot be null!", BAD_REQUEST.getStatusCode());
        }
        logger.debug("OAuthGrant: insert requested for current user and grant with scope {} and type {}",
                grant.getScope(), grant.getType());
        String path = "/user/current/oauth/grant";
        OAuthGrant result;
        if (OAuthType.TOKEN.equals(grant.getType())) {
            result = hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, null, null, grant,
                    OAuthGrant.class, OAUTH_GRANT_PUBLISHED, OAUTH_GRANT_SUBMITTED_TOKEN);
        } else {
            result = hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, null, null, grant,
                    OAuthGrant.class, OAUTH_GRANT_PUBLISHED, OAUTH_GRANT_SUBMITTED_CODE);
        }
        logger.debug("OAuthGrant: insert proceed for current user and grant with scope {} and type {}. Result id {}",
                grant.getScope(), grant.getType(), result.getId());
        return result;
    }

    @Override
    public OAuthGrant update(long userId, long grantId, OAuthGrant grant) throws HiveClientException {
        if (grant == null) {
            throw new HiveClientException("OAuthGrant cannot be null!", BAD_REQUEST.getStatusCode());
        }
        logger.debug("OAuthGrant: update requested for user with id {}, grant id {} and grant with scope {} and type " +
                "{}", userId, grantId, grant.getScope(), grant.getType());
        String path = "/user/" + userId + "/oauth/grant/" + grantId;
        OAuthGrant result = hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, null, null, grant,
                OAuthGrant.class, OAUTH_GRANT_PUBLISHED, null);
        logger.debug("OAuthGrant: update proceed for user with id {}, grant id {} and grant with scope {} and type " +
                "{}", userId, grantId, grant.getScope(), grant.getType());
        return result;
    }

    @Override
    public OAuthGrant update(long grantId, OAuthGrant grant) throws HiveClientException {
        if (grant == null) {
            throw new HiveClientException("OAuthGrant cannot be null!", BAD_REQUEST.getStatusCode());
        }
        logger.debug("OAuthGrant: update requested for current user, grant id {} and grant with scope {} and type {}",
                grantId, grant.getScope(), grant.getType());
        String path = "/user/current/oauth/grant/" + grantId;
        OAuthGrant result = hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, null, null, grant,
                OAuthGrant.class, OAUTH_GRANT_PUBLISHED, null);
        logger.debug("OAuthGrant: update proceed for current user, grant id {} and grant with scope {} and type {}",
                grantId, grant.getScope(), grant.getType());
        return result;
    }

    @Override
    public void delete(long userId, long grantId) {
        logger.debug("OAuthGrant: delete requested for user id {} and grant id {}", userId, grantId);
        String path = "/user/" + userId + "/oauth/grant/" + grantId;
        hiveContext.getHiveRestClient().execute(path, HttpMethod.DELETE);
        logger.debug("OAuthGrant: delete proceed for user id {} and grant id {}", userId, grantId);
    }

    @Override
    public void delete(long grantId) {
        logger.debug("OAuthGrant: delete requested for current user and grant id {}", grantId);
        String path = "/user/current/oauth/grant/" + grantId;
        hiveContext.getHiveRestClient().execute(path, HttpMethod.DELETE);
        logger.debug("OAuthGrant: delete proceed for current user and grant id {}", grantId);
    }
}
