package com.devicehive.dao;

import com.devicehive.model.OAuthClient;

import java.util.List;

public interface OAuthClientDao {
    int deleteById(Long id);

    OAuthClient getByOAuthId(String oauthId);

    OAuthClient getByName(String name);

    OAuthClient getByOAuthIdAndSecret(String id, String secret);

    OAuthClient find(Long id);

    void persist(OAuthClient oAuthClient);

    OAuthClient merge(OAuthClient existing);

    List<OAuthClient> get(String name,
                          String namePattern,
                          String domain,
                          String oauthId,
                          String sortField,
                          Boolean sortOrderAsc,
                          Integer take,
                          Integer skip);
}
