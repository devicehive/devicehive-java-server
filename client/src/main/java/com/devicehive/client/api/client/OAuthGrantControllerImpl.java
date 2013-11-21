package com.devicehive.client.api.client;


import com.devicehive.client.context.HiveContext;
import com.devicehive.client.model.AccessType;
import com.devicehive.client.model.OAuthGrant;
import com.devicehive.client.model.OAuthType;
import com.google.common.reflect.TypeToken;

import javax.ws.rs.HttpMethod;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.*;

public class OAuthGrantControllerImpl implements OAuthGrantController {

    private final HiveContext hiveContext;

    public OAuthGrantControllerImpl(HiveContext hiveContext) {
        this.hiveContext = hiveContext;
    }

    @Override
    public List<OAuthGrant> list(long userId, Timestamp start, Timestamp end, String clientOauthId, OAuthType type,
                                 String scope, String redirectUri, AccessType accessType, String sortField,
                                 String sortOrder, Integer take, Integer skip) {
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
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, queryParams,
                new TypeToken<List<OAuthGrant>>() {
                }.getType(), OAUTH_GRANT_LISTED);
    }

    @Override
    public List<OAuthGrant> list(Timestamp start, Timestamp end, String clientOauthId, OAuthType type, String scope,
                                 String redirectUri, AccessType accessType, String sortField, String sortOrder,
                                 Integer take, Integer skip) {
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
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, queryParams,
                new TypeToken<List<OAuthGrant>>() {
                }.getType(), OAUTH_GRANT_LISTED);
    }

    @Override
    public OAuthGrant get(long userId, long grantId) {
        String path = "/user/" + userId + "/oauth/grant/" + grantId;
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, OAuthGrant.class,
                OAUTH_GRANT_LISTED);
    }

    @Override
    public OAuthGrant get(long grantId) {
        String path = "/user/current/oauth/grant/" + grantId;
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, OAuthGrant.class,
                OAUTH_GRANT_LISTED);
    }

    @Override
    public OAuthGrant insert(long userId, OAuthGrant grant) {
        String path = "/user/" + userId + "/oauth/grant";
        if (OAuthType.TOKEN.equals(grant.getType())) {
            return hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, null, null, grant, OAuthGrant.class,
                    OAUTH_GRANT_PUBLISHED, OAUTH_GRANT_SUBMITTED_TOKEN);
        } else {
            return hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, null, null, grant, OAuthGrant.class,
                    OAUTH_GRANT_PUBLISHED, OAUTH_GRANT_SUBMITTED_CODE);
        }
    }

    @Override
    public OAuthGrant insert(OAuthGrant grant) {
        String path = "/user/current/oauth/grant";
        if (OAuthType.TOKEN.equals(grant.getType())) {
            return hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, null, null, grant, OAuthGrant.class,
                    OAUTH_GRANT_PUBLISHED, OAUTH_GRANT_SUBMITTED_TOKEN);
        } else {
            return hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, null, null, grant, OAuthGrant.class,
                    OAUTH_GRANT_PUBLISHED, OAUTH_GRANT_SUBMITTED_CODE);
        }
    }

    @Override
    public OAuthGrant update(long userId, long grantId, OAuthGrant grant) {
        String path = "/user/" + userId + "/oauth/grant/" + grantId;
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, null, null, grant, OAuthGrant.class,
                OAUTH_GRANT_PUBLISHED, null);
    }

    @Override
    public OAuthGrant update(long grantId, OAuthGrant grant) {
        String path = "/user/current/oauth/grant/" + grantId;
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, null, null, grant, OAuthGrant.class,
                OAUTH_GRANT_PUBLISHED, null);
    }

    @Override
    public void delete(long userId, long grantId) {
        String path = "/user/" + userId + "/oauth/grant/" + grantId;
        hiveContext.getHiveRestClient().execute(path, HttpMethod.DELETE);
    }

    @Override
    public void delete(long grantId) {
        String path = "/user/current/oauth/grant/" + grantId;
        hiveContext.getHiveRestClient().execute(path, HttpMethod.DELETE);
    }
}
