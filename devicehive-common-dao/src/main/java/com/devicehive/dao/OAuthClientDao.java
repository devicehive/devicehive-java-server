package com.devicehive.dao;

import com.devicehive.vo.OAuthClientVO;

import java.util.List;

public interface OAuthClientDao {
    int deleteById(Long id);

    OAuthClientVO getByOAuthId(String oauthId);

    OAuthClientVO getByName(String name);

    OAuthClientVO getByOAuthIdAndSecret(String id, String secret);

    OAuthClientVO find(Long id);

    void persist(OAuthClientVO oAuthClient);

    OAuthClientVO merge(OAuthClientVO existing);

    List<OAuthClientVO> list(String name,
                          String namePattern,
                          String domain,
                          String oauthId,
                          String sortField,
                          Boolean sortOrderAsc,
                          Integer take,
                          Integer skip);
}
