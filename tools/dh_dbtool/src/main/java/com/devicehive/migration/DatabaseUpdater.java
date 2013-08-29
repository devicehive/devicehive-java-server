package com.devicehive.migration;

import com.googlecode.flyway.core.Flyway;
import com.googlecode.flyway.core.api.FlywayException;
import com.googlecode.flyway.core.api.MigrationInfo;
import com.googlecode.flyway.core.api.MigrationVersion;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Set;

public class DatabaseUpdater {
    private static final HelpFormatter HELP_FORMATTER = new HelpFormatter();
    private static final String SELECT_NUMBER_OF_TABLES_IN_SCHEMA = "select count(table_name) as number " +
            "from information_schema.tables " +
            "where table_schema = ? and has_table_privilege(? ,quote_ident(table_schema) || '.' || quote_ident" +
            "(table_name), 'INSERT');";
    private Options options = new Options();
    private Flyway flyway = new Flyway();

    public void execute(PrintStream out, PrintStream err, String... args) {
        initOptions(err);
        CommandLine command = parse(err, args);
        if (command == null) {
            return;
        }

        if (command.hasOption(Constants.HELP_OPTION)) {
            help();
            return;
        }

        if (command.hasOption(Constants.PRINT_VERSION_OPTION)) {
            try {
                printCurrentVersion(out, err,
                        command.getOptionValue(Constants.DATABASE_JDBC_URL),
                        command.getOptionValue(Constants.DATABASE_LOGIN),
                        command.getOptionValue(Constants.USER_PASSWORD),
                        command.getOptionValue(Constants.DATABASE_SCHEMA));
            } catch (FlywayException e) {
                out.println(e.getMessage());
                if (e.getCause() != null) {
                    out.println(e.getCause().getMessage());
                }
            }
            return;
        }

        if (command.hasOption(Constants.MIGRATE_OPTION)) {
            try {
                migrate(err,
                        command.getOptionValue(Constants.DATABASE_JDBC_URL),
                        command.getOptionValue(Constants.DATABASE_LOGIN),
                        command.getOptionValue(Constants.USER_PASSWORD),
                        command.getOptionValue(Constants.DATABASE_SCHEMA));
            } catch (FlywayException e) {
                out.println(e.getMessage());
                if (e.getCause() != null) {
                    out.println(e.getCause().getMessage());
                }
            }
            return;
        }

        help();
    }

    private void initOptions(PrintStream err) {
        initFlagsOptions(err);
        initArgumentsOptions(err);
    }

    private void initFlagsOptions(PrintStream err) {
        Properties props = new Properties();
        try (InputStream is = Main.class.getResourceAsStream(Constants.FLAGS_FILE)) {
            props.load(is);
            Set<String> propertiesNames = props.stringPropertyNames();
            for (String propertyName : propertiesNames) {
                options.addOption(propertyName, false, props.getProperty(propertyName));
            }
        } catch (IOException e) {
            err.println(Constants.READ_OPTIONS_EXCEPTION);
        }
    }

    private void initArgumentsOptions(PrintStream err) {
        Properties props = new Properties();
        try (InputStream is = Main.class.getResourceAsStream(Constants.ARGUMENTS_FILE)){
            props.load(is);
            Set<String> propertiesNames = props.stringPropertyNames();
            for (String propertyName : propertiesNames) {
                Option option = OptionBuilder.withArgName(propertyName).
                        withDescription(props.getProperty(propertyName))
                        .hasArg()
                        .create(propertyName);
                options.addOption(option);
            }
        } catch (IOException e) {
            err.println(Constants.READ_OPTIONS_EXCEPTION);
        }
    }

    private CommandLine parse(PrintStream err, String... args) {
        CommandLineParser parser = new BasicParser();
        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            err.print(Constants.PARSE_EXCEPTION_MESSAGE);
            help();
        }
        return null;
    }

    private void help() {
        HELP_FORMATTER.printHelp(Constants.MIGRATION_TOOL_NAME, options);
    }

    private void migrate(PrintStream err, String url, String user, String password,
                         String schema) {
        if (url == null || user == null) {
            err.print(Constants.NO_URL_OR_USER_MESSAGE);
            return;
        }
        flyway.setDataSource(url, user, password);
        if (schema != null) {
            flyway.setSchemas(schema);
        }
        if (flyway.info().current() == null) {
            try (Connection connection = flyway.getDataSource().getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(SELECT_NUMBER_OF_TABLES_IN_SCHEMA)){
                if (schema == null) {
                    statement.setString(1, flyway.getSchemas()[0]);
                } else {
                    statement.setString(1, schema);
                }
                statement.setString(2, user);
                ResultSet result = null;
                try {
                    result = statement.executeQuery();
                    result.next();
                    Integer tablesNumber = result.getInt("number");
                    if (tablesNumber != 0) {
                        err.print(Constants.NOT_EMPTY_DATABASE_WARNING_MESSAGE);
                        return;
                    }
                } finally {
                    if (result != null) {
                        try {
                            result.close();
                        } catch (SQLException e) {
                            err.print(Constants.DATABASE_ACCESS_ERROR_MESSAGE + e.getMessage());
                        }
                    }
                }
                }
            } catch (SQLException e) {
                err.print(Constants.UNEXPECTED_EXCEPTION + e.getMessage());
                return;
            }
        }
        flyway.migrate();
    }

    private void printCurrentVersion(PrintStream out, PrintStream err, String url, String user, String password,
                                     String schema) {
        if (url == null || user == null) {
            err.print(Constants.NO_URL_OR_USER_MESSAGE);
            return;
        }
        flyway.setDataSource(url, user, password);
        if (schema != null) {
            flyway.setSchemas(schema);
        }
        MigrationInfo currentVersionInfo = flyway.info().current();
        out.println("CURRENT VERSION INFO:");
        if (currentVersionInfo != null) {
            out.println("Checksum: " + currentVersionInfo.getChecksum());
            out.println("Description: " + currentVersionInfo.getDescription());
            MigrationVersion lastAvailableVersion = currentVersionInfo.getVersion();
            out.println("Version: " + currentVersionInfo.getVersion());
            out.println("State: " + currentVersionInfo.getState());
            out.println("Installed on: " + currentVersionInfo.getInstalledOn());
            out.println("Type: " + currentVersionInfo.getType());
            out.println("Script: " + currentVersionInfo.getScript());

            MigrationInfo[] lastVersionInfo = flyway.info().pending();
            for (MigrationInfo info : lastVersionInfo) {
                if (!lastAvailableVersion.equals(info.getVersion())) {
                    lastAvailableVersion = info.getVersion();
                }
            }
            if (currentVersionInfo.getVersion().equals(lastAvailableVersion)) {
                out.println(Constants.LATEST_DB_VERSION_USED_MESSAGE);
            } else {
                out.println(Constants.MIGRATION_REQUIRED_MESSAGE);
            }
        } else {
            MigrationInfo[] lastVersionInfo = flyway.info().pending();
            out.print(Constants.EMPTY_DATABASE_MESSAGE + lastVersionInfo[lastVersionInfo.length - 1].getVersion());
            out.print(Constants.MIGRATION_REQUIRED_MESSAGE);
        }
    }

}
