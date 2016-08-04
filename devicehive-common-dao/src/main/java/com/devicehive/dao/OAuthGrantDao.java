package com.devicehive.dao;

import com.devicehive.model.User;
import com.devicehive.vo.OAuthGrantVO;
import com.devicehive.vo.UserVO;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

public interface OAuthGrantDao {
    OAuthGrantVO getByIdAndUser(UserVO user, Long grantId);

    OAuthGrantVO getById(Long grantId);

    int deleteByUserAndId(UserVO user, Long grantId);

    OAuthGrantVO getByCodeAndOAuthID(String authCode, String clientOAuthID);

    OAuthGrantVO find(Long id);

    void persist(OAuthGrantVO oAuthGrant);

    OAuthGrantVO merge(OAuthGrantVO existing);

    List<OAuthGrantVO> list(@NotNull UserVO user,
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
