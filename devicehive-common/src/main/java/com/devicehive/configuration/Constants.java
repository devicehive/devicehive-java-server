package com.devicehive.configuration;

/*
 * #%L
 * DeviceHive Common Dao Interfaces
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Locale;

public class Constants {

    //Internal use
    public static final String UTF8 = "UTF-8";
    public static final Locale LOCALE = Locale.getDefault();
    public static final String PERSISTENCE_UNIT = "default";
    public static final String WEBSOCKET_SERVER_URL = "websocket.url";
    public static final String REST_SERVER_URL = "rest.url";
    public static final String NULL_SUBSTITUTE = "";
    public static final long MIN_WAIT_TIMEOUT = 0L;
    public static final String DEFAULT_WAIT_TIMEOUT = "30";
    public static final long MAX_WAIT_TIMEOUT = 60L;
    public static final String MAX_LOGIN_ATTEMPTS = "user.login.maxAttempts";
    public static final int INITIAL_LOGIN_ATTEMPTS = 0;
    public static final int MAX_LOGIN_ATTEMPTS_DEFAULT = 10;
    public static final String LAST_LOGIN_TIMEOUT = "user.login.lastTimeout"; // 1 hour
    public static final long LAST_LOGIN_TIMEOUT_DEFAULT = 1000; // 1 hour
    public static final int WEBSOCKET_MAX_BUFFER_SIZE = 10 * 1024;
    public static final int WEBSOCKET_TIMEOUT = 4 * 60 * 1000;
    public static final Integer DEFAULT_TAKE = 20;
    public static final Integer DEFAULT_SKIP = 0;
    public static final Boolean DEFAULT_RETURN_UPDATED_COMMANDS = false;
    public static final String DEFAULT_TAKE_STR = "100";
    public static final String DEFAULT_SKIP_STR = "0";
    public static final String CURRENT_USER = "current";
    public static final String BASIC_AUTH_SCHEME = "Basic";
    public static final String TOKEN_SCHEME = "Bearer";
    public static final String AUTH_DEVICE_ID_HEADER = "Auth-DeviceID";
    public static final String AUTH_DEVICE_KEY_HEADER = "Auth-DeviceKey";
    public static final ByteBuffer PING = ByteBuffer.wrap("devicehive-ping".getBytes(Charset.forName(UTF8)));
    public static final String DEVICE_OFFLINE_STATUS = "Offline";
    public static final String ENV_SECRET_VAR_NAME = "JWT_SECRET";
    public static final String DB_SECRET_VAR_NAME = "jwt.secret";
    public static final String REQUEST_TOPIC = "request_topic";
    public static final String SUBSCRIPTION_TOPIC = "subscription_update";
    public final static String USER_ID = "userId";
    public final static String X_FORWARDED_PROTO_HEADER_NAME = "X-Forwarded-Proto";
    public final static String X_FORWARDED_PORT_HEADER_NAME = "X-Forwarded-Port";
    
    //API constants
    public static final String INFO = "info";
    public static final String CACHE_INFO = "cacheInfo";
    public static final String CLUSTER_INFO = "clusterInfo";
    public static final String CONFIGURATION = "configuration";
    public static final String DEVICE_ID = "deviceId";
    public static final String DEVICE_IDS = "deviceIds";
    public static final String NAMES = "names";
    public static final String TIMESTAMP = "timestamp";
    public static final String START_TIMESTAMP = "start";
    public static final String END_TIMESTAMP = "end";
    public static final String LIMIT = "limit";
    public static final String COMMAND_ID = "commandId";
    public static final String RETURN_COMMANDS = "returnCommands";
    public static final String RETURN_UPDATED_COMMANDS = "returnUpdatedCommands";
    public static final String RETURN_NOTIFICATIONS = "returnNotifications";
    public static final String NOTIFICATION = "notification";
    public static final String NOTIFICATIONS = "notifications";
    public static final String NOTIFICATION_ID = "notificationId";
    public static final String USER = "user";
    public static final String USERS = "users";
    public static final String COMMAND = "command";
    public static final String COMMANDS = "commands";
    public static final String COMMAND_UPDATE = "command_update";
    public static final String PAYLOAD = "payload";
    public static final String SUBSCRIPTION_ID = "subscriptionId";
    public static final String SUBSCRIPTIONS = "subscriptions";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String NAME_PATTERN = "namePattern";
    public static final String VALUE = "value";
    public static final String LABEL = "label";
    public static final String STATUS = "status";
    public static final String NETWORK = "network";
    public static final String NETWORKS = "networks";
    public static final String NETWORK_ID = "networkId";
    public static final String NETWORK_IDS = "networkIds";
    public static final String NETWORK_NAME = "networkName";
    public static final String DEVICE_TYPE = "deviceType";
    public static final String DEVICE_TYPES = "deviceTypes";
    public static final String DEVICE_TYPE_ID = "deviceTypeId";
    public static final String DEVICE_TYPE_IDS = "deviceTypeIds";
    public static final String SORT_FIELD = "sortField";
    public static final String SORT_ORDER = "sortOrder";
    public static final String TAKE = "take";
    public static final String SKIP = "skip";
    public static final String DOMAIN = "domain";
    public static final String LOGIN = "login";
    public static final String DEVICE = "device";
    public static final String DEVICES = "devices";
    public static final String TYPE = "type";
    public static final String COUNT = "count";
    public static final String DESCRIPTION = "description";
    public static final String PARAMETERS = "parameters";
    public static final String FORCE = "force";
    public static final long DEFAULT_SESSION_TIMEOUT = 1200000;

    public static final String ANY = "*";
    public static final String GET_NETWORK = "GetNetwork";
    public static final String GET_DEVICE_TYPE = "GetDeviceType";
    public static final String GET_DEVICE = "GetDevice";
    public static final String GET_DEVICE_NOTIFICATION = "GetDeviceNotification";
    public static final String GET_DEVICE_COMMAND = "GetDeviceCommand";
    public static final String GET_PLUGIN = "GetPlugin";
    public static final String REGISTER_DEVICE = "RegisterDevice";
    public static final String CREATE_DEVICE_NOTIFICATION = "CreateDeviceNotification";
    public static final String CREATE_DEVICE_COMMAND = "CreateDeviceCommand";
    public static final String UPDATE_DEVICE_COMMAND = "UpdateDeviceCommand";
    public static final String GET_CURRENT_USER = "GetCurrentUser";
    public static final String UPDATE_CURRENT_USER = "UpdateCurrentUser";
    public static final String MANAGE_USER = "ManageUser";
    public static final String MANAGE_CONFIGURATION = "ManageConfiguration";
    public static final String MANAGE_NETWORK = "ManageNetwork";
    public static final String MANAGE_DEVICE_TYPE = "ManageDeviceType";
    public static final String MANAGE_TOKEN = "ManageToken";
    public static final String MANAGE_PLUGIN = "ManagePlugin";
    
    public static final String BOOTSTRAP_SERVERS = "bootstrap.servers";
    public static final String NOTIFICATION_TOPIC_NAME = "device_notification";
    public static final String COMMAND_TOPIC_NAME = "device_command";
    public static final String COMMAND_UPDATE_TOPIC_NAME = "device_command_update";
    public static final String ZOOKEEPER_CONNECT = "zookeeper.connect";
    public static final String GROOP_ID = "group.id";
    public static final String ZOOKEEPER_SESSION_TIMEOUT_MS = "session.timeout.ms";
    public static final String ZOOKEEPER_CONNECTION_TIMEOUT_MS = "connection.timeout.ms";
    public static final String ZOOKEEPER_SYNC_TIME_MS = "sync.time.ms";
    public static final String AUTO_COMMIT_INTERVAL_MS = "auto.commit.interval.ms";
    public static final String THREADS_COUNT = "threads.count";

    public static final String WELCOME_MESSAGE = "The DeviceHive RESTful API is now running, please refer to documentation to get the list of available resources.";

}
