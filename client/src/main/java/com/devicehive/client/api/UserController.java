package com.devicehive.client.api;


import com.devicehive.client.model.User;
import com.devicehive.client.model.UserNetwork;

import java.util.List;

public interface UserController {

    //user
    List<User> listUsers(String login, String loginPattern, Integer role, Integer status, String sortField,
                         String sortOrder, Integer take, Integer skip);

    User getUser(String id);

    User insertUser(User user);

    void updateUser(String id, User user);

    void deleteUser(long id);

    UserNetwork getNetwork(long userId, long networkId);

    void assignNetwork(long userId, long networkId);

    void unassignNetwork(long userId, long networkId);
}
