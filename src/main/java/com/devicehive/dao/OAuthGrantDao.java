package com.devicehive.dao;

import com.devicehive.model.OAuthGrant;
import com.devicehive.model.User;

public interface OAuthGrantDao {
    OAuthGrant getByIdAndUser(User user, Long grantId);
    OAuthGrant getById(Long grantId);
    int deleteByUserAndId(User user, Long grantId);
    OAuthGrant getByCodeAndOAuthID(String authCode, String clientOAuthID);
}
