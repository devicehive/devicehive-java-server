package com.devicehive.client.impl;


import com.devicehive.client.UserController;
import com.devicehive.client.impl.context.RestAgent;
import com.devicehive.client.model.User;
import com.devicehive.client.model.UserNetwork;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.devicehive.client.model.exceptions.HiveException;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.*;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

class UserControllerImpl implements UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserControllerImpl.class);
    private final RestAgent restAgent;

    UserControllerImpl(RestAgent restAgent) {
        this.restAgent = restAgent;
    }

    @Override
    public List<User> listUsers(String login, String loginPattern, Integer role, Integer status, String sortField,
                                String sortOrder, Integer take, Integer skip) throws HiveException {
        logger.debug("User: list requested with following parameters: login {}, login pattern {}, role {}, status {}," +
                        " sort field {}, sort order {}, take {}, skip {}", login, loginPattern, role, status, sortField,
                sortOrder, take, skip);
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
        Type type = new TypeToken<List<User>>() {
            private static final long serialVersionUID = -8980491502416082021L;
                }.getType();
        List<User> result = restAgent.execute(path, HttpMethod.GET, null,
                queryParams, null, type, null, USERS_LISTED);
        logger.debug("User: list proceed with following parameters: login {}, login pattern {}, role {}, status {}," +
                        " sort field {}, sort order {}, take {}, skip {}", login, loginPattern, role, status, sortField,
                sortOrder, take, skip);
        return result;
    }

    @Override
    public User getUser(long id) throws HiveException {
        logger.debug("User: get requested for user with id {}", id);
        String path = "/user/" + id;
        User result = restAgent.execute(path, HttpMethod.GET, null, null,
                User.class,
                USER_PUBLISHED);
        logger.debug("User: get request proceed for user with id {}", id);
        return result;
    }

    @Override
    public User getCurrent() throws HiveException {
        logger.debug("User: get requested for current user");
        String path = "/user/current";
        User result = restAgent.execute(path, HttpMethod.GET, null, null,
                User.class,
                USER_PUBLISHED);
        logger.debug("User: get request proceed for current user");
        return result;
    }

    @Override
    public User insertUser(User user) throws HiveException {
        if (user == null) {
            throw new HiveClientException("User cannot be null!", BAD_REQUEST.getStatusCode());
        }
        logger.debug("User: insert requested for user with params: login {}, role {}, status {}", user.getLogin(),
                user.getRole(), user.getStatus());
        String path = "/user";
        User result = restAgent.execute(path, HttpMethod.POST, null, null, user,
                User.class,
                USER_UPDATE, USER_SUBMITTED);
        logger.debug("User: insert proceed for user with params: login {}, role {}, status {}. Id {}", user.getLogin(),
                user.getRole(), user.getStatus(), user.getId());
        return result;
    }

    @Override
    public void updateUser(User user) throws HiveException {
        if (user == null) {
            throw new HiveClientException("User cannot be null!", BAD_REQUEST.getStatusCode());
        }
        if (user.getId() == null) {
            throw new HiveClientException("User id cannot be null!", BAD_REQUEST.getStatusCode());
        }
        logger.debug("User: update requested for user with params: id {}, login {}, role {}, status {}",
                user.getId(), user.getLogin(), user.getRole(), user.getStatus());
        String path = "/user/" + user.getId();
        restAgent.execute(path, HttpMethod.PUT, null, user, USER_UPDATE);
        logger.debug("User: update proceed for user with params: id {}, login {}, role {}, status {}",
                user.getId(), user.getLogin(), user.getRole(), user.getStatus());
    }

    @Override
    public void updateCurrent(User user) throws HiveException {
        if (user == null) {
            throw new HiveClientException("User cannot be null!", BAD_REQUEST.getStatusCode());
        }
        logger.debug("User: update requested for current user with params: login {}, role {}, status {}",
                user.getLogin(), user.getRole(), user.getStatus());
        String path = "/user/current";
        restAgent.execute(path, HttpMethod.PUT, null, user, USER_UPDATE);
        logger.debug("User: update proceed for current user with params: login {}, role {}, status {}",
                user.getLogin(), user.getRole(), user.getStatus());
    }

    @Override
    public void deleteUser(long id) throws HiveException {
        logger.debug("User: delete requested for user with id {}", id);
        String path = "/user/" + id;
        restAgent.execute(path, HttpMethod.DELETE);
        logger.debug("User: delete proceed for user with id {}", id);
    }

    @Override
    public UserNetwork getNetwork(long userId, long networkId) throws HiveException {
        logger.debug("User: getNetwork requested for user with id {} and network with id {}", userId, networkId);
        String path = "/user/" + userId + "/network/" + networkId;
        UserNetwork result = restAgent.execute(path, HttpMethod.GET, null,
                UserNetwork.class,
                NETWORKS_LISTED);
        logger.debug("User: getNetwork proceed for user with id {} and network with id {}", userId, networkId);
        return result;
    }

    @Override
    public void assignNetwork(long userId, long networkId) throws HiveException {
        logger.debug("User: assignNetwork requested for user with id {} and network with id {}", userId, networkId);
        String path = "/user/" + userId + "/network/" + networkId;
        JsonObject stub = new JsonObject();
        restAgent.execute(path, HttpMethod.PUT, null, stub, null);
        logger.debug("User: assignNetwork proceed for user with id {} and network with id {}", userId, networkId);
    }

    @Override
    public void unassignNetwork(long userId, long networkId) throws HiveException {
        logger.debug("User: unassignNetwork requested for user with id {} and network with id {}", userId, networkId);
        String path = "/user/" + userId + "/network/" + networkId;
        restAgent.execute(path, HttpMethod.DELETE);
        logger.debug("User: unassignNetwork proceed for user with id {} and network with id {}", userId, networkId);
    }
}
