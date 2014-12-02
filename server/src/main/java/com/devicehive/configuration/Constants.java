package com.devicehive.configuration;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Locale;

public class Constants {

    //Internal use
    public static final String UTF8 = "UTF-8";
    public static final Locale LOCALE = Locale.getDefault();
    public static final String PERSISTENCE_UNIT = "devicehive";
    public static final String API_VERSION = "1.3.0";
    public static final String WEBSOCKET_SERVER_URL = "websocket.url";
    public static final String REST_SERVER_URL = "rest.url";
    public static final long NULL_ID_SUBSTITUTE = -1L;
    public static final String NULL_SUBSTITUTE = "";
    public static final String DEFAULT_WAIT_TIMEOUT = "30";
    public static final long MAX_WAIT_TIMEOUT = 60L;
    public static final String MAX_LOGIN_ATTEMPTS = "user.login.maxAttempts";
    public static final int INITIAL_LOGIN_ATTEMPTS = 0;
    public static final int MAX_LOGIN_ATTEMPTS_DEFAULT = 10;
    public static final String LAST_LOGIN_TIMEOUT = "user.login.lastTimeout"; // 1 hour
    public static final long LAST_LOGIN_TIMEOUT_DEFAULT = 1000; // 1 hour
    public static final String WEBSOCKET_SESSION_PING_TIMEOUT = "websocket.ping.timeout";
    public static final long WEBSOCKET_SESSION_PING_TIMEOUT_DEFAULT = 2 * 60 * 1000; //2 minutes
    public static final int WEBSOCKET_MAX_BUFFER_SIZE = 10 * 1024;
    public static final String DEVICE_ACTIVITY_MAP = "DEVICE_ACTIVITY_MAP";
    public static final Integer DEFAULT_TAKE = 1000;
    public static final String CURRENT_USER = "current";
    public static final String BASIC_AUTH_SCHEME = "Basic";
    public static final String OAUTH_AUTH_SCEME = "Bearer";
    public static final String OAUTH_IDENTITY = "Identity";
    public static final String OAUTH_ACCESS_TOKEN = "access_token";
    public static final String OAUTH_STATE = "state";
    public static final String OAUTH_CODE = "code";
    public static final String OAUTH_EXPIRES_IN = "expires_in";
    public static final String AUTH_DEVICE_ID_HEADER = "Auth-DeviceID";
    public static final String AUTH_DEVICE_KEY_HEADER = "Auth-DeviceKey";
    public static final ByteBuffer PING = ByteBuffer.wrap("devicehive-ping".getBytes(Charset.forName(UTF8)));
    //API constants
    public static final String DEVICE_ID = "deviceId";
    public static final String DEVICE_GUID = "deviceGuid";
    public static final String DEVICE_GUIDS = "deviceGuids";
    public static final String DEVICE_KEY = "deviceKey";
    public static final String NAMES = "names";
    public static final String TIMESTAMP = "timestamp";
    public static final String COMMAND_ID = "commandId";
    public static final String NOTIFICATION = "notification";
    public static final String COMMAND = "command";
    public static final String SUBSCRIPTION = "subscription";
    public static final String SUBSCRIPTION_ID = "subscriptionId";
    public static final String USER_ID = "userId";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String VALUE = "value";
    public static final String NAME_PATTERN = "namePattern";
    public static final String VERSION = "version";
    public static final String SORT_FIELD = "sortField";
    public static final String SORT_ORDER = "sortOrder";
    public static final String TAKE = "take";
    public static final String SKIP = "skip";
    public static final String WAIT_TIMEOUT = "waitTimeout";
    public static final String START = "start";
    public static final String END = "end";
    public static final String STATUS = "status";
    public static final String GRID_INTERVAL = "gridInterval";
    public static final String NETWORK_ID = "networkId";
    public static final String NETWORK_NAME = "networkName";
    public static final String DEVICE_CLASS_ID = "deviceClassId";
    public static final String DEVICE_CLASS_NAME = "deviceClassName";
    public static final String DEVICE_CLASS_VERSION = "deviceClassVersion";
    public static final String NETWORK = "network";
    public static final String DEVICE_CLASS = "deviceClass";
    public static final String EQUIPMENT = "equipment";
    public static final String CODE = "code";
    public static final String DOMAIN = "domain";
    public static final String OAUTH_ID = "oauthId";
    public static final String CLIENT_OAUTH_ID = "clientOAuthId";
    public static final String TYPE = "type";
    public static final String SCOPE = "scope";
    public static final String REDIRECT_URI = "redirectUri";
    public static final String ACCESS_TYPE = "accessType";
    public static final String GRANT_TYPE = "grant_type";
    public static final String CLIENT_ID = "client_id";
    public static final String USERNAME = "username";
    public static final String LOGIN = "login";
    public static final String LOGIN_PATTERN = "loginPattern";
    public static final String ROLE = "role";
    public static final String DEVICE = "device";

    public static final String GOOGLE_IDENTITY_PROVIDER_ID = "google.identity.provider.id";
    public static final String GOOGLE_IDENTITY_CLIENT_ID = "google.identity.client.id";
    public static final String FACEBOOK_IDENTITY_PROVIDER_ID = "facebook.identity.provider.id";
    public static final String FACEBOOK_IDENTITY_CLIENT_ID = "facebook.identity.client.id";
    public static final String GITHUB_IDENTITY_PROVIDER_ID = "github.identity.provider.id";
    public static final String GITHUB_IDENTITY_ACCESS_TOKEN_ENDPOINT = "github.identity.access.token.endpoint";
    public static final String GITHUB_IDENTITY_CLIENT_ID = "github.identity.client.id";
    public static final String GITHUB_IDENTITY_CLIENT_SECRET = "github.identity.client.secret";

}