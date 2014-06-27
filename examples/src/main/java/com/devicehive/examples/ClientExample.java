package com.devicehive.examples;


import com.devicehive.client.CommandsController;
import com.devicehive.client.HiveClient;
import com.devicehive.client.HiveFactory;
import com.devicehive.client.HiveMessageHandler;
import com.devicehive.client.model.AccessKey;
import com.devicehive.client.model.Device;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.JsonStringWrapper;
import com.devicehive.client.model.User;
import com.devicehive.client.model.exceptions.HiveException;
import com.devicehive.exceptions.ExampleException;
import com.google.gson.JsonObject;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.devicehive.constants.Constants.USE_SOCKETS;

/**
 * Client example represents base client features. It sends commands and receive notifications.
 */
public class ClientExample extends Example {
    private static final String LOGIN = "login";
    private static final String LOGIN_DESCRIPTION = "User login.";
    private static final String PASSWORD = "password";
    private static final String PASSWORD_DESCRIPTION = "User password.";
    private static final String ACCESS_KEY = "ak";
    private static final String ACCESS_KEY_DESCRIPTION = "User access key.";
    private final CommandLine commandLine;
    private final HiveClient hiveClient;

    /**
     * Constructor. Creates hiveClient instance.
     *
     * @param out  out PrintStream
     * @param args commandLine arguments
     * @throws HiveException    if unable to create hiveClient instance
     * @throws ExampleException if server URL cannot be parsed
     */
    public ClientExample(PrintStream out, String... args) throws HiveException, ExampleException {
        super(out, args);
        commandLine = getCommandLine();
        hiveClient = HiveFactory
                .createClient(getServerUrl(), commandLine.hasOption(USE_SOCKETS), Example.impl, Example.impl);
    }

    /**
     * Entrance point.
     *
     * @param args command line arguments
     */
    public static void main(String... args) {
        try {
            Example clientExample = new ClientExample(System.out, args);
            clientExample.run();
        } catch (HiveException | ExampleException | IOException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public Options makeOptionsSet() {
        Options options = new Options();
        options.addOption(LOGIN, true, LOGIN_DESCRIPTION);
        options.addOption(PASSWORD, true, PASSWORD_DESCRIPTION);
        options.addOption(ACCESS_KEY, true, ACCESS_KEY_DESCRIPTION);
        return options;
    }

    /**
     * Creates user with provided login and password to authorize
     *
     * @return user with provided login and password
     */
    private User createUser() {
        User user = new User();
        user.setLogin(commandLine.getOptionValue(LOGIN));
        user.setPassword(commandLine.getOptionValue(PASSWORD));
        return user;
    }

    /**
     * Creates access key with provided key to authorize
     *
     * @return access key with provided key
     */
    private AccessKey createKey() {
        AccessKey key = new AccessKey();
        key.setKey(commandLine.getOptionValue(ACCESS_KEY));
        return key;
    }

    /**
     * Shows how to authorize using access key or user. Subscribes for the notifications. Sends a dummy command to
     * all available devices every 10 seconds. The task runs for 10 minutes.
     *
     * @throws HiveException
     * @throws ExampleException
     * @throws IOException
     */
    @Override
    public void run() throws HiveException, ExampleException, IOException {
        try {
            if (commandLine.hasOption(ACCESS_KEY)) {
                AccessKey key = createKey();
                hiveClient.authenticate(key.getKey());
            } else {
                User user = createUser();
                hiveClient.authenticate(user.getLogin(), user.getPassword());
            }
            hiveClient.getNotificationsController().subscribeForNotifications(null, null);
            ScheduledExecutorService commandsExecutor = Executors.newSingleThreadScheduledExecutor();
            CommandTask commandTask = new CommandTask();
            commandsExecutor.scheduleAtFixedRate(commandTask, 10, 10, TimeUnit.SECONDS);
            Thread.currentThread().join(TimeUnit.MINUTES.toMillis(10));
        } catch (InterruptedException e) {
            throw new ExampleException(e.getMessage(), e);
        } finally {
            hiveClient.close();
        }
    }

    /**
     * Commands creator. Sends commands to all available devices.
     */
    private class CommandTask implements Runnable {
        private final DeviceCommand command = new DeviceCommand();
        private final CommandsController cc;
        private final List<Device> allAvailableDevices;
        private final HiveMessageHandler<DeviceCommand> commandsHandler = new HiveMessageHandler<DeviceCommand>() {
            @Override
            public void handle(DeviceCommand message) {
                print("Command proceed: %s. Id: %s", message.getCommand(), message.getId());
            }
        };

        private CommandTask() throws HiveException {
            command.setCommand("example_command");
            JsonObject commandParams = new JsonObject();
            commandParams.addProperty("command_param_1", "val.1: " + UUID.randomUUID());
            commandParams.addProperty("command_param_2", "val.2: " + UUID.randomUUID());
            command.setParameters(new JsonStringWrapper(commandParams.toString()));
            cc = hiveClient.getCommandsController();
            allAvailableDevices = hiveClient.getDeviceController().listDevices(null, null, null,
                    null, null, null, null, null, null, null, null, null);
        }

        @Override
        public void run() {
            try{
                for (Device device : allAvailableDevices) {
                    cc.insertCommand(device.getId(), command, commandsHandler);
                    print("The command {} will be sent to device {}", command.getParameters(), device.getId());
                }
            } catch (HiveException e) {
                print("Unable to send a command to device");
            }
        }
    }
}
