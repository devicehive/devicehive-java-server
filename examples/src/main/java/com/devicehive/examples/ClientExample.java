package com.devicehive.examples;


import com.devicehive.client.CommandsController;
import com.devicehive.client.HiveClient;
import com.devicehive.client.HiveFactory;
import com.devicehive.client.model.*;
import com.devicehive.client.model.exceptions.HiveException;
import com.devicehive.exceptions.ExampleException;
import com.google.gson.JsonObject;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    public ClientExample(PrintStream err, PrintStream out, String... args) throws HiveException, ExampleException {
        super(out, args);
        commandLine = getCommandLine();
        hiveClient = HiveFactory.createClient(getServerUrl(), commandLine.hasOption(USE_SOCKETS));
    }

    public static void main(String... args) {
        try {
            Example clientExample = new ClientExample(System.err, System.out, args);
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
            commandsExecutor.scheduleAtFixedRate(new CommandTask(), 10, 10, TimeUnit.SECONDS);
            ScheduledExecutorService notificationsExecutor = Executors.newSingleThreadScheduledExecutor();
            notificationsExecutor.scheduleAtFixedRate(new NotificationTask(), 10, 10, TimeUnit.SECONDS);
            Thread.currentThread().join(TimeUnit.MINUTES.toMillis(10));
        } catch (InterruptedException e) {
            throw new ExampleException(e.getMessage(), e);
        } finally {
            hiveClient.close();
        }
    }

    private class CommandTask implements Runnable {
        @Override
        public void run() {
            try {
                List<Device> allAvailableDevices = hiveClient.getDeviceController().listDevices(null, null, null,
                        null, null, null, null, null, null, null, null, null);
                CommandsController cc = hiveClient.getCommandsController();
                DeviceCommand command = new DeviceCommand();
                command.setCommand("example_command");
                JsonObject commandParams = new JsonObject();
                commandParams.addProperty("command_param_1", "val.1: " + UUID.randomUUID());
                commandParams.addProperty("command_param_2", "val.2: " + UUID.randomUUID());
                command.setParameters(new JsonStringWrapper(commandParams.toString()));
                for (Device device : allAvailableDevices) {
                    cc.insertCommand(device.getId(), command);
                    print("The command {} will be sent to device {}", command.getParameters(), device.getId());
                }
            } catch (HiveException e) {
                print("Unable to list devices");
            }
        }
    }

    private class NotificationTask implements Runnable {
        @Override
        public void run() {
            try {
                Queue<Pair<String, DeviceNotification>> notificationsQueue = hiveClient.getNotificationsQueue();
                while (!notificationsQueue.isEmpty()) {
                    print("Processing notification {}", notificationsQueue.poll().getRight());
                }
            } catch (HiveException e) {
                print(e.getMessage());
            }
        }
    }
}
