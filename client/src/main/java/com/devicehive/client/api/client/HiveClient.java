package com.devicehive.client.api.client;


import com.devicehive.client.model.ApiInfo;

import java.io.Closeable;

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
    ApiInfo getInfo();

    /**
     * Authenticates client as user (by login and password). Permissions will be determined by user's role.
     *
     * @param login    login
     * @param password password
     */
    void authenticate(String login, String password);

    /**
     * Authenticates client by access key. Permissions will be determined by the access key permissions.
     *
     * @param key access key
     */
    void authenticate(String key);

    /**
     * Return new instance of access key controller
     *
     * @return access key controller
     */
    AccessKeyController getAccessKeyController();

    /**
     * Return new instance of command controller
     *
     * @return command controller
     */
    CommandsController getCommandsController();

    /**
     * Return new instance of device controller
     *
     * @return device controller
     */
    DeviceController getDeviceController();

    /**
     * Return new instance of network controller
     *
     * @return network controller
     */
    NetworkController getNetworkController();

    /**
     * Return new instance of notification controller
     *
     * @return notification controller
     */
    NotificationsController getNotificationsController();

    /**
     * Return new instance of user controller.
     *
     * @return user controller
     */
    UserController getUserController();

}
