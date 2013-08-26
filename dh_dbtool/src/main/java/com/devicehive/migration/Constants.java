package com.devicehive.migration;


public class Constants {
    public static final String FLAGS_FILE = "/flags.properties";

    public static final String ARGUMENTS_FILE = "/arguments.properties";

    public static final String MIGRATION_TOOL_NAME = "dh_dbtool";

    public static final String READ_OPTIONS_EXCEPTION = "Unexpected read options exception";

    public static final String HELP_OPTION = "h";

    public static final String MIGRATE_OPTION = "m";

    public static final String PRINT_VERSION_OPTION = "pv";

    public static final String DATABASE_JDBC_URL= "url";

    public static final String DATABASE_LOGIN = "user";

    public static final String USER_PASSWORD = "password";

    public static final String NO_URL_OR_USER_MESSAGE = "Database connection url and user are required";

    public static final String LATEST_DB_VERSION_USED_MESSAGE = "\nYou use latest database version. Migration is not " +
            "required";

    public static final String MIGRATION_REQUIRED_MESSAGE = "\nYour database version is outdated. You need to update your " +
            "database.";

    public static final String PARSE_EXCEPTION_MESSAGE = "Unable to parse command line arguments.";
}
