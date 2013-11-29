package com.devicehive.client.config;

public class Constants {
    /**
     * Should not be changed. Defines request and responses encoding.
     */
    public static final String CURRENT_CHARSET = "UTF-8";
    /**
     * Authorization header for devices
     */
    public static final String DEVICE_ID_HEADER = "Auth-DeviceID";
    /**
     * Authorization header for devices
     */
    public static final String DEVICE_KEY_HEADER = "Auth-DeviceKey";
    /**
     * Substitute for device id in "for all subscription"
     */
    public static final String FOR_ALL_SUBSTITUTE = "This is surrogate for subscription for all available";
    /**
     * Default wait timeout for requests, that contains waitTimeout param (for example, DeviceCommand: wait)
     */
    public static final int WAIT_TIMEOUT = 30;
    /**
     * Timeout for websocket ping/pongs. If no pongs received during this timeout, reconnection will be started.
     * Notice, that reconnection will be started, if some request did not received any message during response
     * timeout, reconnection action will be started.
     */
    public static final long WEBSOCKET_PING_TIMEOUT = 2L;
    /**
     * Pool size for subscriptions threads
     */
    public static final int SUBSCRIPTIONS_THREAD_POOL_SIZE = 100;
    /**
     * Await termination timeout for subscriptions
     */
    public static final Integer AWAIT_TERMINATION_TIMEOUT = 10;
    /**
     * Interval that defines how often websocket subscriptions will be cleaned.
     */
    public static final Integer CLEANER_TASK_INTERVAL = 30;// in minutes
    /**
     * Required version of server API
     */
    public static final String REQUIRED_VERSION_OF_API = "1.3.0";

}
