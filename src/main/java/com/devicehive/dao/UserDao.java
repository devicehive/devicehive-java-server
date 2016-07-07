package com.devicehive.dao;

import com.devicehive.model.Network;
import com.devicehive.model.User;

import java.util.Optional;

public interface UserDao {
    Optional<User> findByName(String name);
    User findByGoogleName(String name);
    User findByFacebookName(String name);
    User findByGithubName(String name);
    Optional<User> findByIdentityName(String login, String googleLogin, String facebookLogin, String githubLogin);
    long hasAccessToNetwork(User user, Network network);
    long hasAccessToDevice(User user, String deviceGuid);
    User getWithNetworksById(long id);
    int deleteById(long id);
}
