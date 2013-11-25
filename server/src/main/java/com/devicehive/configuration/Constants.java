package com.devicehive.configuration;

public class Constants {

    public static final String UTF8 = "UTF-8";

    public static final String PERSISTENCE_UNIT = "devicehive";

    public static final String API_VERSION = "1.3.0";

    public static final String WEBSOCKET_SERVER_URL = "websocket.url";

    public static final String REST_SERVER_URL = "rest.url";

    public static final long DEVICE_NOTIFICATION_NULL_ID_SUBSTITUTE = -1L;

    public static final long DEVICE_COMMAND_NULL_ID_SUBSTITUTE = -1L;

    public static final String DEFAULT_WAIT_TIMEOUT = "30";

    public static final long MAX_WAIT_TIMEOUT = 60L;

    public static final String MAX_LOGIN_ATTEMPTS = "user.login.maxAttempts";

    public static final int MAX_LOGIN_ATTEMPTS_DEFALUT = 10;

    public static final String LAST_LOGIN_TIMEOUT = "user.login.lastTimeout"; // 1 hour

    public static final long LAST_LOGIN_TIMEOUT_DEFAULT = 60 * 60 * 1000; // 1 hour

    public static final String WEBSOCKET_SESSION_PING_TIMEOUT = "websocket.ping.timeout";

    public static final long WEBSOCKET_SESSION_PING_TIMEOUT_DEFAULT = 2 * 60 * 1000; //2 minutes

    public static final String DEVICE_ACTIVITY_MAP = "DEVICE_ACTIVITY_MAP";

    public static final Integer DEFAULT_TAKE = 1000;

    public static final String CURRENT_USER = "current";

    public static final String KEY_AUTH = "Bearer";

    public static final String DEBUG_MODE = "debugMode";

    public static final Boolean ENABLE_DEBUG = true;

}
