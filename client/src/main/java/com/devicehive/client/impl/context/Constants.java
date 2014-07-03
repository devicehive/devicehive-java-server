package com.devicehive.client.impl.context;

import java.nio.charset.Charset;

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
     * Required version of server API
     */
    public static final String REQUIRED_VERSION_OF_API = "1.3.0";
    public static final String WAIT_TIMEOUT_PARAM = "waitTimeout";
    public static final String DEVICE_GUIDS = "deviceGuids";
    public static final String NAMES = "names";
    public static final String TIMESTAMP = "timestamp";
    public static final String EXPECTED_RESPONSE_STATUS = "success";
    public static final String STATUS = "status";
    public static final String CODE = "code";
    public static final String ERROR = "error";
    public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    public static final String SEPARATOR = ",";
}
