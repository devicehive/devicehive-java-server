package com.devicehive.migration;


public class Constants {
    public static final String FLAGS_FILE = "/flags.properties";

    public static final String ARGUMENTS_FILE = "/arguments.properties";

    public static final String MIGRATION_TOOL_NAME = "dh_dbtool-1.3.0.0";

    public static final String READ_OPTIONS_EXCEPTION = "Unexpected read options exception";

    public static final String UNEXPECTED_EXCEPTION = "Unexpected exception.";

    public static final String DATABASE_ACCESS_ERROR_MESSAGE = "Database access error occurs: ";

    public static final String HELP_OPTION = "help";

    public static final String MIGRATE_OPTION = "migrate";

    public static final String PRINT_VERSION_OPTION = "version";

    public static final String DATABASE_JDBC_URL= "url";

    public static final String DATABASE_LOGIN = "user";

    public static final String USER_PASSWORD = "password";

    public static final String DATABASE_SCHEMA = "schema";

    public static final String NO_URL_OR_USER_MESSAGE = "Database connection url and user are required";

    public static final String LATEST_DB_VERSION_USED_MESSAGE = "\nYou use latest database version. Migration is not " +
            "required";

    public static final String EMPTY_DATABASE_MESSAGE = "\nNo migrations have been provided yet. The latest " +
            "available version is: ";

    public static final String MIGRATION_REQUIRED_MESSAGE = "\nYour database version is outdated. You need to update your " +
            "database.";

    public static final String NOT_EMPTY_DATABASE_WARNING_MESSAGE = "Unable to provide migration while database is not " +
            "empty. Please, clear the database";

    public static final String PARSE_EXCEPTION_MESSAGE = "Unable to parse command line arguments.";
}
