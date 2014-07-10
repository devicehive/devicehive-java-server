package com.devicehive.client.impl;


import com.devicehive.client.OAuthClientController;
import com.devicehive.client.impl.context.RestAgent;
import com.devicehive.client.model.OAuthClient;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.devicehive.client.model.exceptions.HiveException;
import com.google.common.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.*;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

class OAuthClientControllerImpl implements OAuthClientController {
    private static final Logger logger = LoggerFactory.getLogger(OAuthClientControllerImpl.class);
    private final RestAgent restAgent;

    OAuthClientControllerImpl(RestAgent restAgent) {
        this.restAgent = restAgent;
    }

    @Override
    public List<OAuthClient> list(String name, String namePattern, String domain, String oauthId, String sortField,
                                  String sortOrder, Integer take, Integer skip) throws HiveException {
        logger.debug("OAuthClient: list requested with following parameters: name {}, name pattern {}, domain {}, " +
                        "oauth id {}, sort field {}, sort order {}, take {}, skip {}", name, namePattern, domain, oauthId,
                sortField, sortOrder, take, skip);
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
        List<OAuthClient> result = restAgent.execute(path, HttpMethod.GET, null,
                queryParams,
                new TypeToken<List<OAuthClient>>() {
                }.getType(), OAUTH_CLIENT_LISTED);
        logger.debug(
                "OAuthClient: list request proceed for following parameters: name {}, name pattern {}, domain {}, " +
                        "oauth id {}, sort field {}, sort order {}, take {}, skip {}", name, namePattern, domain,
                oauthId, sortField, sortOrder, take, skip);
        return result;
    }

    @Override
    public OAuthClient get(long id) throws HiveException {
        logger.debug("OAuthClient: get requested for id {}", id);
        String path = "/oauth/client/" + id;
        OAuthClient result = restAgent
                .execute(path, HttpMethod.GET, null, OAuthClient.class, OAUTH_CLIENT_LISTED);
        logger.debug("OAuthClient: get request proceed for id {}. Result name {], domain {}, subnet {}, " +
                        "redirect uri {} ", id, result.getName(), result.getDomain(), result.getSubnet(),
                result.getRedirectUri());
        return result;
    }

    @Override
    public OAuthClient insert(OAuthClient client) throws HiveException {
        if (client == null) {
            throw new HiveClientException("OAuthClient cannot be null!", BAD_REQUEST.getStatusCode());
        }
        logger.debug("OAuthClient: insert requested for client with name {}, domain {], subnet {}, redirect uri {}",
                client.getName(), client.getDomain(), client.getSubnet(), client.getRedirectUri());
        String path = "/oauth/client";
        OAuthClient result = restAgent.execute(path, HttpMethod.POST, null,
                null, client,
                OAuthClient.class, OAUTH_CLIENT_SUBMITTED, OAUTH_CLIENT_PUBLISHED);
        logger.debug("OAuthClient: insert proceed for client with name {}, domain {], subnet {}, " +
                        "redirect uri {}. Result id {}", client.getName(), client.getDomain(), client.getSubnet(),
                client.getRedirectUri(), result.getId());
        return result;
    }

    @Override
    public void update(OAuthClient client) throws HiveException {
        if (client == null) {
            throw new HiveClientException("OAuthClient cannot be null!", BAD_REQUEST.getStatusCode());
        }
        if (client.getId() == null) {
            throw new HiveClientException("OAuthClient id cannot be null!", BAD_REQUEST.getStatusCode());
        }
        logger.debug("OAuthClient: update requested for client with id {}, name {}, domain {], subnet {}, " +
                        "redirect uri {}", client.getId(), client.getName(), client.getDomain(), client.getSubnet(),
                client.getRedirectUri());
        String path = "/oauth/client/" + client.getId();
        restAgent.execute(path, HttpMethod.PUT, null, client,
                OAUTH_CLIENT_SUBMITTED);
        logger.debug("OAuthClient: update proceed for client with id {}, name {}, domain {], subnet {}, " +
                        "redirect uri {}", client.getId(), client.getName(), client.getDomain(), client.getSubnet(),
                client.getRedirectUri());
    }

    @Override
    public void delete(long id) throws HiveException {
        logger.debug("OAuthClient: delete requested for client with id {}", id);
        String path = "/oauth/client/" + id;
        restAgent.execute(path, HttpMethod.DELETE);
        logger.debug("OAuthClient: delete proceed for client with id {}", id);
    }
}
