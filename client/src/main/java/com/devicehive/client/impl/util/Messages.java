package com.devicehive.client.impl.util;

public final class Messages {
    public static final String ALREADY_AUTHENTICATED = "Already authenticated";
    public static final String FORM_PARAMS_ARE_NULL = "Form params cannot be null!";
    public static final String INCORRECT_RESPONSE_TYPE = "Incorrect type of response!";
    public static final String NO_RESPONSES_FROM_SERVER = "Server does not respond!";
    public static final String INCORRECT_ACCESS_TYPE = "Incorrect access type";
    public static final String INVALID_OAUTH_GRANT_TYPE = "Invalid oauth grant type";
    public static final String INVALID_HIVE_PRINCIPAL = "Invalid HivePrincipal was passed";
    public static final String PARSING_MICROSECONDS_ERROR = "Error occurred during microseconds parsing";
    public static final String INCORRECT_TIMESTAMP_FORMAT = "Incorrect timestamp format: %s";
    public static final String INVALID_USER_ROLE = "Invalid user role";
    public static final String INVALID_USER_STATUS = "Invalid user status";
    public static final String SEVERAL_ACTIVE_JSON_POLICIES = "Only one JSON policy is allowed";
    public static final String NOT_A_JSON = "Not a JSON object";
    public static final String INCORRECT_SERVER_URL = "Incorrect server URL!";
    public static final String UNKNOWN_RESPONSE = "Unknown response";
    public static final String CONNECTION_LOST = "Try to send request when connection is lost";

    private Messages() {
    }
}
