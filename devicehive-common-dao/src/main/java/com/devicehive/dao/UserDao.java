package com.devicehive.dao;

import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.UserVO;
import com.devicehive.vo.UserWithNetworkVO;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

public interface UserDao {
    Optional<UserVO> findByName(String name);

    UserVO findByGoogleName(String name);

    UserVO findByFacebookName(String name);

    UserVO findByGithubName(String name);

    Optional<UserVO> findByIdentityName(String login, String googleLogin, String facebookLogin, String githubLogin);

    long hasAccessToNetwork(UserVO user, NetworkVO network);

    long hasAccessToDevice(UserVO user, String deviceGuid);

    UserWithNetworkVO getWithNetworksById(long id);

    int deleteById(long id);

    UserVO find(Long id);

    void persist(UserVO user);

    UserVO merge(UserVO existing);

    void unassignNetwork(@NotNull UserVO existingUser, @NotNull long networkId);

    List<UserVO> getList(String login, String loginPattern, Integer role, Integer status, String sortField,
                       Boolean sortOrderAsc, Integer take, Integer skip);
}
