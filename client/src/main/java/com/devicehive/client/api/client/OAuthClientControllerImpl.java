package com.devicehive.client.api.client;


import com.devicehive.client.context.HiveContext;
import com.devicehive.client.model.OAuthClient;
import com.google.common.reflect.TypeToken;

import javax.ws.rs.HttpMethod;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.*;

public class OAuthClientControllerImpl implements OAuthClientController {
    final HiveContext hiveContext;

    public OAuthClientControllerImpl(HiveContext hiveContext) {
        this.hiveContext = hiveContext;
    }

    @Override
    public List<OAuthClient> list(String name, String namePattern, String domain, String oauthId, String sortField,
                                  String sortOrder, Integer take, Integer skip) {
        String path = "/oauth/client";
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("name", name);
        queryParams.put("namePattern", namePattern);
        queryParams.put("domain", domain);
        queryParams.put("oauthId", oauthId);
        queryParams.put("sortField", sortField);
        queryParams.put("sortOrder", sortOrder);
        queryParams.put("take", take);
        queryParams.put("skip", skip);
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, queryParams,
                new TypeToken<List<OAuthClient>>() {
                }.getType(), OAUTH_CLIENT_LISTED);
    }

    @Override
    public OAuthClient get(long id) {
        String path = "/oauth/client/" + id;
        return hiveContext.getHiveRestClient()
                .execute(path, HttpMethod.GET, null, OAuthClient.class, OAUTH_CLIENT_LISTED);
    }

    @Override
    public OAuthClient insert(OAuthClient client) {
        String path = "/oauth/client";
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, null, null, client, OAuthClient.class,
                OAUTH_CLIENT_SUBMITTED, OAUTH_CLIENT_PUBLISHED);
    }

    @Override
    public void update(long id, OAuthClient client) {
        String path = "/oauth/client/" + id;
        hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, null, client, OAUTH_CLIENT_SUBMITTED);
    }

    @Override
    public void delete(long id) {
        String path = "/oauth/client/" + id;
        hiveContext.getHiveRestClient().execute(path, HttpMethod.DELETE);
    }
}
