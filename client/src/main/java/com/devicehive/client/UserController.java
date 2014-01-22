package com.devicehive.client;


import com.devicehive.client.model.User;
import com.devicehive.client.model.UserNetwork;

import java.util.List;

/**
 * Client side controller for user: <i>/user</i>
 * See <a href="http://www.devicehive.com/restful/#Reference/User">DeviceHive RESTful API: User</a> for
 * details.
 * Transport declared in the hive context will be used.
 */
public interface UserController {

    /**
     * Queries list of users using following criteria:
     * See: <a href="http://www.devicehive.com/restful#Reference/User/listt">DeviceHive RESTful
     * API: User: list</a> for more details.
     *
     * @param login        user login ignored, when loginPattern is specified
     * @param loginPattern login pattern (LIKE %VALUE%) user login will be ignored, if not null
     * @param role         User's role ADMIN - 0, CLIENT - 1
     * @param status       ACTIVE - 0 (normal state, user can log on) , LOCKED_OUT - 1 (locked for multiple log in
     *                     failures), DISABLED - 2 , DELETED - 3;
     * @param sortField    either of "login", "loginAttempts", "role", "status", "lastLogin"
     * @param sortOrder    either ASC or DESC
     * @param take         Number of records to take
     * @param skip         Number of records to skip
     * @return List of users
     */
    List<User> listUsers(String login, String loginPattern, Integer role, Integer status, String sortField,
                         String sortOrder, Integer take, Integer skip);

    /**
     * Gets information about user.
     * See: <a href="http://www.devicehive.com/restful#Reference/User/get">DeviceHive RESTful
     * API: User: get</a> for more details.
     *
     * @param id user identifier
     * @return user associated with request identifier
     */
    User getUser(long id);

    /**
     * Gets information about current user.
     * See: <a href="http://www.devicehive.com/restful#Reference/User/get">DeviceHive RESTful
     * API: User: get</a> for more details.
     *
     * @return current user
     */
    User getUser();

    /**
     * Creates new user.
     * See: <a href="http://www.devicehive.com/restful#Reference/User/insert">DeviceHive RESTful
     * API: User: insert</a> for more details.
     *
     * @param user user to be inserted
     * @return User resource with id and last log in timestamp
     */
    User insertUser(User user);

    /**
     * Updates an existing user.
     * See: <a href="http://www.devicehive.com/restful#Reference/User/update">DeviceHive RESTful
     * API: User: update</a> for more details.
     *
     * @param id   user identifier
     * @param user user resource with update info
     */
    void updateUser(long id, User user);

    /**
     * Updates current user.
     * See: <a href="http://www.devicehive.com/restful#Reference/User/update">DeviceHive RESTful
     * API: User: update</a> for more details.
     *
     * @param user user resource with update info
     */
    void updateUser(User user);

    /**
     * Deletes an existing user.
     * See: <a href="http://www.devicehive.com/restful#Reference/User/delete">DeviceHive RESTful
     * API: User: delete</a> for more details.
     *
     * @param id user identifier
     */
    void deleteUser(long id);

    /**
     * Gets information about user/network association.
     * See: <a href="http://www.devicehive.com/restful#Reference/User/getNetwork">DeviceHive RESTful
     * API: User: getNetwork</a> for more details.
     *
     * @param userId    user identifier
     * @param networkId network identifier
     * @return If successful, this method returns UserNetwork association.
     */
    UserNetwork getNetwork(long userId, long networkId);

    /**
     * Associates network with the user.
     * See: <a href="http://www.devicehive.com/restful#Reference/User/assignNetwork">DeviceHive RESTful
     * API: User: assignNetwork</a> for more details.
     *
     * @param userId    user identifier
     * @param networkId network identifier
     */
    void assignNetwork(long userId, long networkId);

    /**
     * Breaks association between network and user.
     *
     * @param userId    user identifier
     * @param networkId network identifier
     */
    void unassignNetwork(long userId, long networkId);
}
