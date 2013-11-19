package com.devicehive.client.api.client;

import com.devicehive.client.context.HiveContext;
import com.devicehive.client.model.AccessToken;

import java.util.HashMap;
import java.util.Map;

public class OAuthTokenControllerImpl implements OAuthTokenController{

    private final HiveContext hiveContext;

    public OAuthTokenControllerImpl(HiveContext hiveContext) {
        this.hiveContext = hiveContext;
    }

    @Override
    public AccessToken requestAccessToken(String grantType, String code, String redirectUri, String clientId,
                                          String scope, String login, String password) {
        String path = "/oauth2/token";
        Map<String, String> formParams = new HashMap<>();
        formParams.put("grant_type", grantType);
        formParams.put("code", code);
        formParams.put("redirect_uri", redirectUri);
        formParams.put("client_id", clientId);
        formParams.put("scope", scope);
        formParams.put("username", login);
        formParams.put("password", password);
        return hiveContext.getHiveRestClient().executeForm(path, formParams, AccessToken.class, null);
    }
}
