package com.devicehive.client.api;


import com.devicehive.client.context.HiveContext;
import com.devicehive.client.model.User;
import com.devicehive.client.model.UserNetwork;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonObject;

import javax.ws.rs.HttpMethod;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.*;

public class UserControllerImpl implements UserController {
    private final HiveContext hiveContext;

    public UserControllerImpl(HiveContext hiveContext) {
        this.hiveContext = hiveContext;
    }

    @Override
    public List<User> listUsers(String login, String loginPattern, Integer role, Integer status, String sortField,
                                String sortOrder, Integer take, Integer skip) {
        String path = "/user";
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("login", login);
        queryParams.put("loginPattern", loginPattern);
        queryParams.put("role", role);
        queryParams.put("status", status);
        queryParams.put("sortField", sortField);
        queryParams.put("sortOrder", sortOrder);
        queryParams.put("take", take);
        queryParams.put("skip", skip);
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, queryParams, null, new
                TypeToken<List<User>>() {
                }.getType(), null, USERS_LISTED);
    }

    @Override
    public User getUser(String id) {   //for getCurrent support
        String path = "/user/" + id;
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, null, User.class, USER_PUBLISHED);
    }

    @Override
    public User insertUser(User user) {
        String path = "/user";
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, null, null, user, User.class,
                USER_UPDATE, USER_SUBMITTED);
    }

    @Override
    public void updateUser(String id, User user) {
        String path = "/user/" + id;
        hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, null, user, USER_UPDATE);
    }

    @Override
    public void deleteUser(long id) {
        String path = "/user/" + id;
        hiveContext.getHiveRestClient().execute(path, HttpMethod.DELETE);
    }

    @Override
    public UserNetwork getNetwork(long userId, long networkId) {
        String path = "/user/" + userId + "/network/" + networkId;
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, UserNetwork.class, NETWORKS_LISTED);
    }

    @Override
    public void assignNetwork(long userId, long networkId) {
        String path = "/user/" + userId + "/network/" + networkId;
        JsonObject stub = new JsonObject();
        hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, null, stub, null);
    }

    @Override
    public void unassignNetwork(long userId, long networkId) {
        String path = "/user/" + userId + "/network/" + networkId;
        hiveContext.getHiveRestClient().execute(path, HttpMethod.DELETE);
    }
}
