package com.devicehive.client;


import com.devicehive.client.model.ApiInfo;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.exceptions.HiveException;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Closeable;
import java.util.Queue;

/**
 * Hive client that represents the number of controller getters methods. Controllers are used only to delegate
 * methods with similar logic to some container. The separation of client controllers is equal or similar to the
 * server's controller.
 */
public interface HiveClient extends Closeable {

    /**
     * Requests API information
     *
     * @return API info
     */
    ApiInfo getInfo() throws HiveException;

    /**
     * Authenticates client as user (by login and password). Permissions will be determined by user's role.
     *
     * @param login    login
     * @param password password
     */
    void authenticate(String login, String password) throws HiveException;

    /**
     * Authenticates client by access key. Permissions will be determined by the access key permissions.
     *
     * @param key access key
     */
    void authenticate(String key) throws HiveException;

    /**
     * Return new instance of access key controller
     *
     * @return access key controller
     */
    AccessKeyController getAccessKeyController() throws HiveException;

    /**
     * Return new instance of command controller
     *
     * @return command controller
     */
    CommandsController getCommandsController() throws HiveException;

    /**
     * Return new instance of device controller
     *
     * @return device controller
     */
    DeviceController getDeviceController() throws HiveException;

    /**
     * Return new instance of network controller
     *
     * @return network controller
     */
    NetworkController getNetworkController() throws HiveException;

    /**
     * Return new instance of notification controller
     *
     * @return notification controller
     */
    NotificationsController getNotificationsController() throws HiveException;

    /**
     * Return new instance of user controller.
     *
     * @return user controller
     */
    UserController getUserController() throws HiveException;

    /**
     * Get notifications queue
     *
     * @return notifications queue
     */
    Queue<Pair<String, DeviceNotification>> getNotificationsQueue() throws HiveException;

    /**
     * Get commands queue
     *
     * @return commands queue
     */
    Queue<Pair<String, DeviceCommand>> getCommandsQueue() throws HiveException;
}
