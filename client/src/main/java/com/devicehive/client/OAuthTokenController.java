package com.devicehive.client;


import com.devicehive.client.model.AccessToken;
import com.devicehive.client.model.exceptions.HiveException;

public interface OAuthTokenController {

    AccessToken requestAccessToken(String grantType, String code, String redirectUri, String clientId, String scope,
                                   String login, String password) throws HiveException;
}
