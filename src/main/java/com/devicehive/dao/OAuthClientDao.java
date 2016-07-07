package com.devicehive.dao;

import com.devicehive.model.OAuthClient;

public interface OAuthClientDao {
    int deleteById(Long id);
    OAuthClient getByOAuthId(String oauthId);
    OAuthClient getByName(String name);
    OAuthClient getByOAuthIdAndSecret(String id, String secret);
}
