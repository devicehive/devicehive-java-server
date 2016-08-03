package com.devicehive.dao;

import com.devicehive.model.User;
import com.devicehive.vo.OAuthGrantVO;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

public interface OAuthGrantDao {
    OAuthGrantVO getByIdAndUser(User user, Long grantId);

    OAuthGrantVO getById(Long grantId);

    int deleteByUserAndId(User user, Long grantId);

    OAuthGrantVO getByCodeAndOAuthID(String authCode, String clientOAuthID);

    OAuthGrantVO find(Long id);

    void persist(OAuthGrantVO oAuthGrant);

    OAuthGrantVO merge(OAuthGrantVO existing);

    List<OAuthGrantVO> list(@NotNull User user,
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
