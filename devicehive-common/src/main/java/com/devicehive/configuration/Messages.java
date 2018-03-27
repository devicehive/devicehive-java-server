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


import java.util.ResourceBundle;

public class Messages {

    public static final String NOT_AUTHORIZED = BidBundle.get("NOT_AUTHORIZED");
    public static final String ACCESS_DENIED = BidBundle.get("ACCESS_DENIED");
    public static final String TWO_OR_MORE_ACTIVE_JSON_POLICIES = BidBundle.get("TWO_OR_MORE_ACTIVE_JSON_POLICIES");
    public static final String NOT_A_JSON_OBJECT = BidBundle.get("NOT_A_JSON_OBJECT");
    public static final String UNPARSEABLE_TIMESTAMP = BidBundle.get("UNPARSEABLE_TIMESTAMP");
    public static final String UNPARSEABLE_SORT_ORDER = BidBundle.get("UNPARSEABLE_SORT_ORDER");
    public static final String SORT_ORDER_IS_NULL = BidBundle.get("SORT_ORDER_IS_NULL");
    public static final String INVALID_REQUEST_PARAMETERS = BidBundle.get("INVALID_REQUEST_PARAMETERS");
    public static final String HEALTH_CHECK_FAILED = BidBundle.get("HEALTH_CHECK_FAILED");
    public static final String DUPLICATE_LABEL_FOUND = BidBundle.get("DUPLICATE_LABEL_FOUND");
    public static final String JSON_SYNTAX_ERROR = BidBundle.get("JSON_SYNTAX_ERROR");
    public static final String CONFIG_NOT_FOUND = BidBundle.get("CONFIG_NOT_FOUND");
    public static final String CONFIGURATION_NAME_REQUIRED = BidBundle.get("CONFIGURATION_NAME_REQUIRED");
    public static final String DEVICE_NOT_FOUND = BidBundle.get("DEVICE_NOT_FOUND");
    public static final String NETWORK_NOT_FOUND = BidBundle.get("NETWORK_NOT_FOUND");
    public static final String DEVICE_TYPES_NOT_FOUND = BidBundle.get("DEVICE_TYPES_NOT_FOUND");
    public static final String DEVICE_TYPE_NOT_FOUND = BidBundle.get("DEVICE_TYPE_NOT_FOUND");
    public static final String NETWORKS_NOT_FOUND = BidBundle.get("NETWORKS_NOT_FOUND");
    public static final String COMMAND_NOT_FOUND = BidBundle.get("COMMAND_NOT_FOUND");
    public static final String NOTIFICATION_NOT_FOUND = BidBundle.get("NOTIFICATION_NOT_FOUND");
    public static final String NOTIFICATION_NOT_FOUND_LOG = BidBundle.get("NOTIFICATION_NOT_FOUND_LOG");
    public static final String GRANT_NOT_FOUND = BidBundle.get("GRANT_NOT_FOUND");
    public static final String USER_NOT_FOUND = BidBundle.get("USER_NOT_FOUND");
    public static final String INVALID_TOPIC_NAME = BidBundle.get("INVALID_TOPIC_NAME");
    public static final String USER_LOGIN_NOT_FOUND = BidBundle.get("USER_LOGIN_NOT_FOUND");
    public static final String USER_NOT_PLUGIN_CREATOR = BidBundle.get("USER_NOT_PLUGIN_CREATOR");
    public static final String USER_NOT_ACTIVE = BidBundle.get("USER_NOT_ACTIVE");
    public static final String BAD_AUTHENTICATION_RESPONSE = BidBundle.get("BAD_AUTHENTICATION_RESPONSE");
    public static final String DEVICES_NOT_FOUND = BidBundle.get("DEVICES_NOT_FOUND");
    public static final String DEVICE_IS_BLOCKED = BidBundle.get("DEVICE_IS_BLOCKED");
    public static final String NO_NOTIFICATIONS_FROM_DEVICE = BidBundle.get("NO_NOTIFICATIONS_FROM_DEVICE");
    public static final String CONFLICT_MESSAGE = BidBundle.get("CONFLICT_MESSAGE");
    public static final String ACCESS_KEY_NOT_FOUND = BidBundle.get("ACCESS_KEY_NOT_FOUND");
    public static final String BAD_USER_IDENTIFIER = BidBundle.get("BAD_USER_IDENTIFIER");
    public static final String UNAUTHORIZED_REASON_PHRASE = BidBundle.get("UNAUTHORIZED_REASON_PHRASE");
    public static final String DEVICE_IS_NOT_CONNECTED_TO_NETWORK = BidBundle.get("DEVICE_IS_NOT_CONNECTED_TO_NETWORK");
    public static final String CLIENT_ID_IS_REQUIRED = BidBundle.get("CLIENT_ID_IS_REQUIRED");
    public static final String INVALID_GRANT_TYPE = BidBundle.get("INVALID_GRANT_TYPE");
    public static final String CAN_NOT_GET_CURRENT_USER = BidBundle.get("CAN_NOT_GET_CURRENT_USER");
    public static final String USER_NETWORK_NOT_FOUND = BidBundle.get("USER_NETWORK_NOT_FOUND");
    public static final String USER_DEVICE_TYPE_NOT_FOUND = BidBundle.get("USER_DEVICE_TYPE_NOT_FOUND");
    public static final String LABEL_IS_REQUIRED = BidBundle.get("LABEL_IS_REQUIRED");
    public static final String ACTIONS_ARE_REQUIRED = BidBundle.get("ACTIONS_ARE_REQUIRED");
    public static final String UNKNOWN_ACTION = BidBundle.get("UNKNOWN_ACTION");
    public static final String ID_NOT_ALLOWED = BidBundle.get("ID_NOT_ALLOWED");
    public static final String INCORRECT_CREDENTIALS = BidBundle.get("INCORRECT_CREDENTIALS");
    public static final String DEVICE_ID_REQUIRED = BidBundle.get("DEVICE_ID_REQUIRED");
    public static final String EMPTY_DEVICE = BidBundle.get("EMPTY_DEVICE");
    public static final String EMPTY_DEVICE_NAME = BidBundle.get("EMPTY_DEVICE_NAME");
    public static final String EMPTY_COMMAND = BidBundle.get("EMPTY_COMMAND");
    public static final String NOTIFICATION_REQUIRED = BidBundle.get("NOTIFICATION_REQUIRED");
    public static final String NOTIFICATION_ID_REQUIRED = BidBundle.get("NOTIFICATION_ID_REQUIRED");
    public static final String CLIENT_IS_NULL = BidBundle.get("CLIENT_IS_NULL");
    public static final String INVALID_AUTH_CODE = BidBundle.get("INVALID_AUTH_CODE");
    public static final String INVALID_AUTH_REQUEST_PARAMETERS = BidBundle.get("INVALID_AUTH_REQUEST_PARAMETERS");
    public static final String INVALID_URI = BidBundle.get("INVALID_URI");
    public static final String EXPIRED_GRANT = BidBundle.get("EXPIRED_GRANT");
    public static final String CLIENT_REQUIRED = BidBundle.get("CLIENT_REQUIRED");
    public static final String TYPE_REQUIRED = BidBundle.get("TYPE_REQUIRED");
    public static final String REDIRECT_URI_REQUIRED = BidBundle.get("REDIRECT_URI_REQUIRED");
    public static final String SCOPE_REQUIRED = BidBundle.get("SCOPE_REQUIRED");
    public static final String COMMAND_ID_REQUIRED = BidBundle.get("COMMAND_ID_REQUIRED");
    public static final String VALIDATION_FAILED = BidBundle.get("VALIDATION_FAILED");
    public static final String DUPLICATE_LOGIN = BidBundle.get("DUPLICATE_LOGIN");
    public static final String PASSWORD_REQUIRED = BidBundle.get("PASSWORD_REQUIRED");
    public static final String PASSWORD_VALIDATION_FAILED = BidBundle.get("PASSWORD_VALIDATION_FAILED");
    public static final String INTERNAL_SERVER_ERROR = BidBundle.get("INTERNAL_SERVER_ERROR");
    public static final String INCORRECT_ACCESS_TYPE = BidBundle.get("INCORRECT_ACCESS_TYPE");
    public static final String PARSING_MICROSECONDS_ERROR = BidBundle.get("PARSING_MICROSECONDS_ERROR");
    public static final String INVALID_USER_ROLE = BidBundle.get("INVALID_USER_ROLE");
    public static final String USER_ID_REQUIRED = BidBundle.get("USER_ID_REQUIRED");
    public static final String USER_REQUIRED = BidBundle.get("USER_REQUIRED");
    public static final String NETWORK_REQUIRED = BidBundle.get("NETWORK_REQUIRED");
    public static final String NETWORK_ID_REQUIRED = BidBundle.get("NETWORK_ID_REQUIRED");
    public static final String ADMIN_PERMISSIONS_REQUIRED = BidBundle.get("ADMIN_PERMISSIONS_REQUIRED");
    public static final String INVALID_USER_STATUS = BidBundle.get("INVALID_USER_STATUS");
    public static final String INVALID_ACCESS_KEY_TYPE = BidBundle.get("INVALID_ACCESS_KEY_TYPE");
    public static final String DUPLICATE_NETWORK = BidBundle.get("DUPLICATE_NETWORK");
    public static final String INVALID_NETWORK_KEY = BidBundle.get("INVALID_NETWORK_KEY");
    public static final String NO_ACCESS_TO_NETWORK = BidBundle.get("NO_ACCESS_TO_NETWORK");
    public static final String NETWORK_CREATION_NOT_ALLOWED = BidBundle.get("NETWORK_CREATION_NOT_ALLOWED");
    public static final String NETWORK_DELETION_NOT_ALLOWED = BidBundle.get("NETWORK_DELETION_NOT_ALLOWED");
    public static final String DEVICE_TYPE_REQUIRED = BidBundle.get("DEVICE_TYPE_REQUIRED");
    public static final String DEVICE_TYPE_ID_REQUIRED = BidBundle.get("DEVICE_TYPE_ID_REQUIRED");
    public static final String DUPLICATE_DEVICE_TYPE = BidBundle.get("DUPLICATE_DEVICE_TYPE");
    public static final String NO_ACCESS_TO_DEVICE_TYPE = BidBundle.get("NO_ACCESS_TO_DEVICE_TYPE");
    public static final String DEVICE_TYPE_CREATION_NOT_ALLOWED = BidBundle.get("DEVICE_TYPE_CREATION_NOT_ALLOWED");
    public static final String DEVICE_TYPE_DELETION_NOT_ALLOWED = BidBundle.get("DEVICE_TYPE_DELETION_NOT_ALLOWED");
    public static final String DEVICE_TYPE_ASSIGNMENT_NOT_ALLOWED = BidBundle.get("DEVICE_TYPE_ASSIGNMENT_NOT_ALLOWED");
    public static final String PARAMS_NOT_JSON = BidBundle.get("PARAMS_NOT_JSON");
    public static final String NO_NOTIFICATION_PARAMS = BidBundle.get("NO_NOTIFICATION_PARAMS");
    public static final String UNKNOWN_ACTION_REQUESTED_WS = BidBundle.get("UNKNOWN_ACTION_REQUESTED_WS");
    public static final String EMPTY_NAMES = BidBundle.get("EMPTY_NAMES");
    public static final String COLUMN_CANNOT_BE_NULL = BidBundle.get("COLUMN_CANNOT_BE_NULL");
    public static final String FIELD_LENGTH_CONSTRAINT = BidBundle.get("FIELD_LENGTH_CONSTRAINT");
    public static final String PING_ERROR = BidBundle.get("PING_ERROR");
    public static final String SHUTDOWN = BidBundle.get("SHUTDOWN");
    public static final String NO_ACCESS_TO_DEVICE = BidBundle.get("NO_ACCESS_TO_DEVICE");
    public static final String NO_ACCESS_TO_DEVICE_TYPES_OR_NETWORKS = BidBundle.get("NO_ACCESS_TO_DEVICE_TYPES_OR_NETWORKS");
    public static final String NO_NETWORKS_ASSIGNED_TO_USER = BidBundle.get("NO_NETWORKS_ASSIGNED_TO_USER");
    public static final String CANT_DELETE_CURRENT_USER_KEY = BidBundle.get("CANT_DELETE_CURRENT_USER_KEY");
    public static final String CANT_DELETE_LAST_DEFAULT_ACCESS_KEY = BidBundle.get("CANT_DELETE_LAST_DEFAULT_ACCESS_KEY");
    public static final String FORBIDDEN_INSERT_USER = BidBundle.get("FORBIDDEN_INSERT_USER");
    public static final String FORBIDDEN_INSERT_SPECIAL_NOTIFICATION = BidBundle.get("FORBIDDEN_INSERT_SPECIAL_NOTIFICATION");
    public static final String NOTIFICATION_INSERT_FAILED = BidBundle.get("NOTIFICATION_INSERT_FAILED");
    public static final String PAYLOAD_NOT_FOUND = BidBundle.get("PAYLOAD_NOT_FOUND");
    public static final String SUBSCRIPTION_NOT_FOUND = BidBundle.get("SUBSCRIPTION_NOT_FOUND");
    public static final String DEVICE_ID_CONTAINS_INVALID_CHARACTERS = BidBundle.get("DEVICE_ID_CONTAINS_INVALID_CHARACTERS");
    public static final String INVALID_TOKEN = BidBundle.get("INVALID_TOKEN");
    public static final String INVALID_TOKEN_TYPE = BidBundle.get("INVALID_TOKEN_TYPE");
    public static final String EXPIRED_TOKEN = BidBundle.get("EXPIRED_TOKEN");
    public static final String EMPTY_TOKEN = BidBundle.get("EMPTY_TOKEN");
    public static final String PLUGIN_NOT_FOUND = BidBundle.get("PLUGIN_NOT_FOUND");
    public static final String PLUGIN_NOT_ACTIVE = BidBundle.get("PLUGIN_NOT_ACTIVE");
    public static final String PLUGIN_ALREADY_EXISTS = BidBundle.get("PLUGIN_ALREADY_EXISTS");
    public static final String ACTIVE_PLUGIN_UPDATED = BidBundle.get("ACTIVE_PLUGIN_UPDATED");
    public static final String PLUGIN_SUBSCRIPTION_NOT_VALID = BidBundle.get("PLUGIN_SUBSCRIPTION_NOT_VALID");
    public static final String NO_ACCESS_TO_PLUGIN = BidBundle.get("NO_ACCESS_TO_PLUGIN");

    /**
     * Bundle to extract localized strings from property files.
     */
    private static final class BidBundle {

        private static final ResourceBundle bidBundle = ResourceBundle.getBundle("messages", Constants.LOCALE);

        private BidBundle() {
        }

        public static String get(String key) {
            return bidBundle.getString(key);
        }
    }
}
