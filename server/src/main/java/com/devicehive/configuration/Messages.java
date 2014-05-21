package com.devicehive.configuration;


public class Messages {
    public static final String NOT_AUTHORIZED = "{message:\"Not authorized\"}";
    public static final String OAUTH_REALM = "Bearer realm=\"devicehive\"";
    public static final String BASIC_REALM = "Basic realm=\"devicehive\"";
    public static final String TWO_OR_MORE_ACTIVE_JSON_POLICIES = "Two or more active JSON policies";
    public static final String NOT_A_JSON_OBJECT = "Not a JSON object";
    public static final String UNPARSEABLE_TIMESTAMP = "Unparseable timestamp.";
    public static final String UNPARSEABLE_SORT_ORDER = "Unparseable sort order : %s";
    public static final String SORT_ORDER_IS_NULL = "Sort order cannot be null.";
    public static final String INVALID_REQUEST_PARAMETERS = "Invalid request parameters";
    public static final String JSON_SYNTAX_ERROR = "JSON syntax error";
    public static final String DEVICE_NOT_FOUND = "Device with such guid = %s not found";
    public static final String NETWORK_NOT_FOUND = "Network with id = %s not found.";
    public static final String EQUIPMENT_NOT_FOUND = "Equipment with id = %s not found for class with id %s";
    public static final String DEVICE_CLASS_NOT_FOUND = "DeviceClass with id = %s not found.";
    public static final String COMMAND_NOT_FOUND = "Command with id = %s not found";
    public static final String NOTIFICATION_NOT_FOUND = "Notification with id = %s not found";
    public static final String GRANT_NOT_FOUND = "Grant with id = %s not found.";
    public static final String USER_NOT_FOUND = "User with id = %s not found";
    public static final String OAUTH_CLIENT_NOT_FOUND = "OAuth client with id = %s not found";
    public static final String DEVICES_NOT_FOUND = "Devices with such guids wasn't found: {%s}";
    public static final String NO_NOTIFICATIONS_FROM_DEVICE = "No device notifications " +
            "found from device with guid : %s";
    public static final String CONFLICT_MESSAGE = "Error occurred. Please, retry again.";
    public static final String ACCESS_KEY_NOT_FOUND = "Access key not found.";
    public static final String BAD_USER_IDENTIFIER = "Bad user identifier : %s";
    public static final String UNAUTHORIZED_REASON_PHRASE = "Unauthorized";
    public static final String DEVICE_IS_NOT_CONNECTED_TO_NETWORK =
            "Device with guid = %s is not connected to any network";
    public static final String CLIENT_ID_IS_REQUIRED = "Client id is required!";
    public static final String INVALID_GRANT_TYPE = "Invalid grant type!";
    public static final String CAN_NOT_GET_CURRENT_USER = "Can not get current user.";
    public static final String USER_NETWORK_NOT_FOUND = "Network with id %s for user with is %s was not found";
    public static final String LABEL_IS_REQUIRED = "Label is required!";
    public static final String ACTIONS_ARE_REQUIRED = "Actions are required!";
    public static final String UNKNOWN_ACTION = "Unknown action!";
    public static final String OAUTH_TOKEN_LABEL = "OAuth token for: %s";
    public static final String ID_NOT_ALLOWED = "Invalid request parameters. Id cannot be specified.";
    public static final String DEVICE_CLASS_WITH_SUCH_NAME_AND_VERSION_EXISTS =
            "DeviceClass cannot be created. Device class with such name and version already exists";
    public static final String DUPLICATE_EQUIPMENT_ENTRY =
            "Duplicate equipment entry with code = %s for device class with id : %s";
    public static final String UPDATE_PERMANENT_EQUIPMENT = "Unable to update equipment on permanent device class.";
    public static final String INCORRECT_CREDENTIALS = "Invalid credentials";
    public static final String DEVICE_GUID_REQUIRED = "Device guid is required";
    public static final String EMPTY_DEVICE = "Device is empty";
    public static final String EMPTY_DEVICE_KEY = "Device key is required";
    public static final String EMPTY_DEVICE_NAME = "Device name is required";
    public static final String EMPTY_DEVICE_CLASS = "Device class is required";
    public static final String EMPTY_COMMAND = "Command is required";
    public static final String NOTIFICATION_REQUIRED = "Notification is required";
    public static final String DUPLICATE_OAUTH_ID = "OAuth client with such OAuthID already exists!";
    public static final String CLIENT_IS_NULL = "Client cannot be null!";
    public static final String INVALID_AUTH_CODE = "Invalid authorization code";
    public static final String INVALID_URI = "Invalid \"redirect_uri\"";
    public static final String EXPIRED_GRANT = "Expired grant";
    public static final String CLIENT_REQUIRED = "Client field is required";
    public static final String TYPE_REQUIRED = "Type field is required";
    public static final String REDIRECT_URI_REQUIRED = "Redirect URI field is required";
    public static final String SCOPE_REQUIRED = "Scope field is required";
    public static final String COMMAND_ID_REQUIRED = "Command id is required";
    public static final String VALIDATION_FAILED = "Validation failed with following violations: %s";
    public static final String DUPLICATE_LOGIN = "User with such login already exists. Please, select another one";
    public static final String PASSWORD_REQUIRED = "Password is required!";
    public static final String INTERNAL_SERVER_ERROR = "Internal server error";
    public static final String INCORRECT_ACCESS_TYPE = "Invalid access type";
    public static final String PARSING_MICROSECONDS_ERROR = "Error occurred during parsing microseconds";
    public static final String INVALID_USER_ROLE = "Invalid user role";
    public static final String INVALID_USER_STATUS = "Invalid user status";
    public static final String DUPLICATE_NETWORK = "Network cannot be created. Network with such name already exists";
    public static final String INVALID_NETWORK_KEY = "Incorrect network key value";
    public static final String NO_ACCESS_TO_NETWORK = "No access to network!";
    public static final String NETWORK_CREATION_NOT_ALLOWED = "No permissions to create network!";
    public static final String PARAMS_NOT_JSON = "\"parameters\" must be JSON Object!";
    public static final String UNKNOWN_ACTION_REQUESTED_WS = "Unknown action requested: %s";
    public static final String EMPTY_NAMES = "Names field is required to be nonempty";
    public static final String COLUMN_CANNOT_BE_NULL = "%s cannot be null.";
    public static final String FIELD_LENGTH_CONSTRAINT =
            "Field cannot be empty. The length of %s should not be more than %s symbols.";
    public static final String NO_PONGS_FOR_A_LONG_TIME = "No pongs for a long time";
    public static final String SHUTDOWN = "Shutdown";

}
