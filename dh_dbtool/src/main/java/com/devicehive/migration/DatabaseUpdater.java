package com.devicehive.migration;

import com.googlecode.flyway.core.Flyway;
import com.googlecode.flyway.core.api.MigrationInfo;
import com.googlecode.flyway.core.api.MigrationVersion;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Properties;
import java.util.Set;

public class DatabaseUpdater {
    private static final HelpFormatter HELP_FORMATTER = new HelpFormatter();
    private Options options = new Options();
    private Flyway flyway = new Flyway();

    public void execute(PrintStream out, PrintStream err, String ... args){
        DatabaseUpdater updater = new DatabaseUpdater();
        updater.initOptions(err);
        CommandLine command = updater.parse(err, args);
        if (command == null) {
            return;
        }

        if (command.hasOption(Constants.HELP_OPTION)) {
            updater.help();
            return;
        }

        if (command.hasOption(Constants.PRINT_VERSION_OPTION)){
            try{
                updater.printCurrentVersion(out, command.getOptionValue(Constants.DATABASE_JDBC_URL),
                        command.getOptionValue(Constants
                                .DATABASE_LOGIN), command.getOptionValue(Constants.USER_PASSWORD));
            }
            catch(IllegalArgumentException e){
                out.print(e.getMessage());
            }
            return;
        }

        if (command.hasOption(Constants.MIGRATE_OPTION)){
            try{
                updater.migrate(command.getOptionValue(Constants.DATABASE_JDBC_URL), command.getOptionValue(Constants
                        .DATABASE_LOGIN), command.getOptionValue(Constants.USER_PASSWORD));
            }
            catch(IllegalArgumentException e){
                out.print(e.getMessage());
            }
            return;
        }

        updater.help();
    }


    private void initOptions(PrintStream err) {
        initFlagsOptions(err);
        initArgumentsOptions(err);
    }

    private void initFlagsOptions(PrintStream err) {
        Properties props = new Properties();
        InputStream is = Main.class.getResourceAsStream(Constants.FLAGS_FILE);
        try {
            props.load(is);
        } catch (IOException e) {
            err.println(Constants.READ_OPTIONS_EXCEPTION);
        }
        Set<String> propertiesNames = props.stringPropertyNames();
        for (String propertyName : propertiesNames) {
            options.addOption(propertyName, false, props.getProperty(propertyName));
        }
    }

    private void initArgumentsOptions(PrintStream err) {
        Properties props = new Properties();
        InputStream is = Main.class.getResourceAsStream(Constants.ARGUMENTS_FILE);
        try {
            props.load(is);
        } catch (IOException e) {
            err.println(Constants.READ_OPTIONS_EXCEPTION);
        }
        Set<String> propertiesNames = props.stringPropertyNames();
        for (String propertyName : propertiesNames) {
            Option option = OptionBuilder.withArgName(propertyName).
                    withDescription(props.getProperty(propertyName))
                    .hasArg()
                    .create(propertyName);
            options.addOption(option);
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

    private void migrate(String url, String user, String password) {
        if (url == null || user == null) {
            throw new IllegalArgumentException(Constants.NO_URL_OR_USER_MESSAGE);
        }
        flyway.setDataSource(url, user, password);
        flyway.migrate();
    }


    private void printCurrentVersion(PrintStream out, String url, String user, String password) {
        if (url == null || user == null) {
            throw new IllegalArgumentException(Constants.NO_URL_OR_USER_MESSAGE);
        }
        flyway.setDataSource(url, user, password);
        out.println("CURRENT VERSION INFO:");
        MigrationInfo currentVersionInfo = flyway.info().current();
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
    }
}
