package com.devicehive.client.api.client;


import com.devicehive.client.model.AccessType;
import com.devicehive.client.model.OAuthGrant;
import com.devicehive.client.model.OAuthType;

import java.sql.Timestamp;
import java.util.List;

public interface OAuthGrantController {

    List<OAuthGrant> list(long userId, Timestamp start, Timestamp end, String clientOauthId, OAuthType type,
                          String scope, String redirectUri, AccessType accessType, String sortField, String sortOrder,
                          Integer take, Integer skip);

    OAuthGrant get(long userId, long grantId);

    OAuthGrant insert(long userId, OAuthGrant grant);

    OAuthGrant update(long userId, long grantId, OAuthGrant grant);

    void delete(long userId, long grantId);
}
