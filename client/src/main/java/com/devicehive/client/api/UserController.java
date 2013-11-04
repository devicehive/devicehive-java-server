package com.devicehive.client.api;


import com.devicehive.client.model.Network;
import com.devicehive.client.model.User;

import java.util.List;

public interface UserController {

    //user
    List<User> listUsers(String login, String loginPattern, Integer role, Integer status, String sortField,
                         String sortOrder, Integer take, Integer skip);

    User getUser(long id);

    User insertUser(User user);

    void updateUser(long id, User user);

    void deleteUser(long id);

    Network getNetwork(long userId, long networkId);

    void assignNetwork(long userId, long networkId);

    void unassignNetwork(long userId, long networkId);
}
