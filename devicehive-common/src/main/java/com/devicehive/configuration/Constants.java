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
    public static final Integer DEFAULT_TAKE = 100;
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
    //API constants
    public static final String DEVICE_ID = "deviceId";
    public static final String DEVICE_IDS = "deviceIds";
    public static final String NAMES = "names";
    public static final String TIMESTAMP = "timestamp";
    public static final String START_TIMESTAMP = "start";
    public static final String END_TIMESTAMP = "end";
    public static final String LIMIT = "limit";
    public static final String COMMAND_ID = "commandId";
    public static final String NOTIFICATION = "notification";
    public static final String NOTIFICATIONS = "notifications";
    public static final String NOTIFICATION_ID = "notificationId";
    public static final String COMMAND = "command";
    public static final String COMMANDS = "commands";
    public static final String PAYLOAD = "payload";
    public static final String SUBSCRIPTION_ID = "subscriptionId";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String NAME_PATTERN = "namePattern";
    public static final String LABEL = "label";
    public static final String STATUS = "status";
    public static final String NETWORK = "network";
    public static final String NETWORK_ID = "networkId";
    public static final String NETWORK_NAME = "networkName";
    public static final String SORT_FIELD = "sortField";
    public static final String SORT_ORDER = "sortOrder";
    public static final String TAKE = "take";
    public static final String SKIP = "skip";
    public static final String DOMAIN = "domain";
    public static final String LOGIN = "login";
    public static final String DEVICE = "device";
    public static final String DEVICES = "devices";
    public static final long DEFAULT_SESSION_TIMEOUT = 1200000;

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
