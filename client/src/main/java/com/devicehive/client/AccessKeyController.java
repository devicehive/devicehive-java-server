package com.devicehive.client;


import com.devicehive.client.model.AccessKey;
import com.devicehive.client.model.exceptions.HiveException;

import java.util.List;

/**
 * Client side controller for access keys: <i>/user/{userId}/accesskey</i>
 * See <a href="http://www.devicehive.com/restful/#Reference/AccessKey">DeviceHive RESTful API: AccessKey</a> for
 * details.
 * Transport declared in the hive context will be used.
 */
public interface AccessKeyController {

    /**
     * <a href="http://www.devicehive.com/restful#Reference/AccessKey/list">DeviceHive RESTful API: AccessKey: list</a>
     * Gets list of access keys and their permissions.
     *
     * @param userId User identifier.
     * @return If successful, this method returns array of <a href="http://www.devicehive
     * .com/restful#Reference/AccessKey/">AccessKey</a> resources in the response body according to the specification.
     */
    List<AccessKey> listKeys(long userId) throws HiveException;

    /**
     * <a href="http://www.devicehive.com/restful#Reference/AccessKey/list">DeviceHive RESTful API: AccessKey: list</a>
     * Gets list of access keys and their permissions. Uses the 'current' keyword to list access keys of the current
     * user.
     *
     * @return If successful, this method returns array of <a href="http://www.devicehive
     * .com/restful#Reference/AccessKey/">AccessKey</a> resources in the response body according to the specification.
     */
    List<AccessKey> listKeys() throws HiveException;

    /**
     * <a href="http://www.devicehive.com/restful#Reference/AccessKey/get">DeviceHive RESTful API: AccessKey: get</a>
     * Gets information about access key and its permissions.
     *
     * @param userId User identifier.
     * @param keyId  Access key identifier.
     * @return If successful, this method returns an <a href="http://www.devicehive
     * .com/restful#Reference/AccessKey/">AccessKey</a> resource in the response body according to the specification.
     */
    AccessKey getKey(long userId, long keyId) throws HiveException;

    /**
     * <a href="http://www.devicehive.com/restful#Reference/AccessKey/get">DeviceHive RESTful API: AccessKey: get</a>
     * Gets information about access key and its permissions. Uses the 'current' keyword to get access key of the
     * current user.
     *
     * @param keyId Access key identifier.
     * @return If successful, this method returns an <a href="http://www.devicehive
     * .com/restful#Reference/AccessKey/">AccessKey</a> resource in the response body according to the specification.
     */
    AccessKey getKey(long keyId) throws HiveException;

    /**
     * <a href="http://www.devicehive.com/restful#Reference/AccessKey/insert">DeviceHive RESTful API: AccessKey:
     * insert</a>
     * Creates new access key.
     *
     * @param userId User identifier.
     * @return If successful, this method returns an <a href="http://www.devicehive
     * .com/restful#Reference/AccessKey/">AccessKey</a> resource in the response body according to the specification.
     */
    AccessKey insertKey(long userId, AccessKey key) throws HiveException;

    /**
     * <a href="http://www.devicehive.com/restful#Reference/AccessKey/insert">DeviceHive RESTful API: AccessKey:
     * insert</a>
     * Creates new access key. Uses the 'current' keyword to create access key of the current user.
     *
     * @return If successful, this method returns an <a href="http://www.devicehive
     * .com/restful#Reference/AccessKey/">AccessKey</a> resource in the response body according to the specification.
     */
    AccessKey insertKey(AccessKey key) throws HiveException;

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/AccessKey/update">DeviceHive RESTful
     * API: AccessKey: update</a>
     * Updates an existing access key.
     *
     * @param userId User identifier.
     * @param key    Key to be updated
     */
    void updateKey(long userId, AccessKey key) throws HiveException;

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/AccessKey/update">DeviceHive RESTful
     * API: AccessKey: update</a>
     * Updates an existing access key. Use the 'current' keyword to update access key of the current user.
     *
     * @param key Key to be updated
     */
    void updateKey(AccessKey key) throws HiveException;

    /**
     * <a href="http://www.devicehive.com/restful#Reference/AccessKey/delete">DeviceHive RESTful API: AccessKey: delete</a>
     *
     * @param userId User identifier.
     * @param keyId  Access key identifier.
     */
    void deleteKey(long userId, long keyId) throws HiveException;

    /**
     * <a href="http://www.devicehive.com/restful#Reference/AccessKey/delete">DeviceHive RESTful API: AccessKey: delete</a>
     * Deletes current user's access key
     *
     * @param keyId Access key identifier.
     */
    void deleteKey(long keyId) throws HiveException;
}
