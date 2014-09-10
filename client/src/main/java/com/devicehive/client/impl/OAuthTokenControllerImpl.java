package com.devicehive.client.impl;

import com.devicehive.client.OAuthTokenController;
import com.devicehive.client.impl.context.RestAgent;
import com.devicehive.client.model.AccessToken;
import com.devicehive.client.model.exceptions.HiveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

class OAuthTokenControllerImpl implements OAuthTokenController {

    private static final Logger logger = LoggerFactory.getLogger(OAuthTokenControllerImpl.class);
    private final RestAgent restAgent;

    OAuthTokenControllerImpl(RestAgent restAgent) {
        this.restAgent = restAgent;
    }

    @Override
    public AccessToken requestAccessToken(String grantType, String code, String redirectUri, String clientId,
                                          String scope, String login, String password) throws HiveException {
        logger.debug("Access token requested with params: grant type {}, code {}, redirect uri {}, client id {}, " +
                "scope {}, login {}", grantType, code, redirectUri, clientId, scope, login);
        String path = "/oauth2/token";
        Map<String, String> formParams = new HashMap<>();
        formParams.put("grant_type", grantType);
        formParams.put("code", code);
        formParams.put("redirect_uri", redirectUri);
        formParams.put("client_id", clientId);
        formParams.put("scope", scope);
        formParams.put("username", login);
        formParams.put("password", password);
        AccessToken result = restAgent.executeForm(path, formParams, AccessToken.class, null);
        logger.debug("Access token request proceed for params: grant type {}, code {}, redirect uri {}, " +
                "client id {}, scope {}, login {}", grantType, code, redirectUri, clientId, scope, login);
        return result;
    }
}
