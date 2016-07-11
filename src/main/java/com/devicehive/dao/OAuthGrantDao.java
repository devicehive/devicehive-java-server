package com.devicehive.dao;

import com.devicehive.model.OAuthGrant;
import com.devicehive.model.User;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

public interface OAuthGrantDao {
    OAuthGrant getByIdAndUser(User user, Long grantId);

    OAuthGrant getById(Long grantId);

    int deleteByUserAndId(User user, Long grantId);

    OAuthGrant getByCodeAndOAuthID(String authCode, String clientOAuthID);

    OAuthGrant find(Long id);

    void persist(OAuthGrant oAuthGrant);

    OAuthGrant merge(OAuthGrant existing);

    List<OAuthGrant> list(@NotNull User user,
                          Date start,
                          Date end,
                          String clientOAuthId,
                          Integer type,
                          String scope,
                          String redirectUri,
                          Integer accessType,
                          String sortField,
                          Boolean sortOrder,
                          Integer take,
                          Integer skip);
}
