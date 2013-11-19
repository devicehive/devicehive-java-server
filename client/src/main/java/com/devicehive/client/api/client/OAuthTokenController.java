package com.devicehive.client.api.client;


import com.devicehive.client.model.AccessToken;

public interface OAuthTokenController {

    AccessToken requestAccessToken(String grantType, String code, String redirectUri, String clientId, String scope,
                                   String login, String password);
}
