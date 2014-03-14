package com.devicehive.examples;


import com.devicehive.client.HiveClient;
import com.devicehive.client.HiveFactory;
import com.devicehive.client.model.AccessKey;
import com.devicehive.client.model.User;
import com.devicehive.client.model.exceptions.HiveException;
import com.devicehive.exceptions.ExampleException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.io.PrintStream;

import static com.devicehive.constants.Constants.USE_SOCKETS;

public class ClientExample extends Example {
    private static final String LOGIN = "login";
    private static final String LOGIN_DESCRIPTION = "User login.";
    private static final String PASSWORD = "password";
    private static final String PASSWORD_DESCRIPTION = "User password.";
    private static final String ACCESS_KEY = "ak";
    private static final String ACCESS_KEY_DESCRIPTION = "User access key.";
    private final CommandLine commandLine;
    private final HiveClient hiveClient;

    public ClientExample(PrintStream err, PrintStream out, String... args) throws HiveException {
        super(err, out, args);
        commandLine = getCommandLine();
        hiveClient = HiveFactory.createClient(getServerUrl(), commandLine.hasOption(USE_SOCKETS));
    }

    @Override
    public Options makeOptionsSet() {
        Options options = new Options();
        options.addOption(LOGIN, true, LOGIN_DESCRIPTION);
        options.addOption(PASSWORD, true, PASSWORD_DESCRIPTION);
        options.addOption(ACCESS_KEY, true, ACCESS_KEY_DESCRIPTION);
        return options;
    }

    private User createUser() {
        User user = new User();
        user.setLogin(commandLine.getOptionValue(LOGIN));
        user.setPassword(commandLine.getOptionValue(PASSWORD));
        return user;
    }

    private AccessKey createKey() {
        AccessKey key = new AccessKey();
        key.setKey(commandLine.getOptionValue(ACCESS_KEY));
        return key;
    }

    @Override
    public void run() throws HiveException, ExampleException, IOException {
        if (commandLine.hasOption(ACCESS_KEY)) {
            AccessKey key = createKey();
            hiveClient.authenticate(key.getKey());
        } else {
            User user = createUser();
            hiveClient.authenticate(user.getLogin(), user.getPassword());
        }
    }
}
