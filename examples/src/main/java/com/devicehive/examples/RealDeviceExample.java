package com.devicehive.examples;


import com.devicehive.client.CommandsController;
import com.devicehive.client.HiveClient;
import com.devicehive.client.HiveFactory;
import com.devicehive.client.HiveMessageHandler;
import com.devicehive.client.NotificationsController;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.JsonStringWrapper;
import com.devicehive.client.model.SubscriptionFilter;
import com.devicehive.client.model.exceptions.HiveException;
import com.devicehive.exceptions.ExampleException;
import com.google.gson.JsonObject;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.io.PrintStream;
import java.sql.Timestamp;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.devicehive.constants.Constants.USE_SOCKETS;

public class RealDeviceExample extends Example {
    private static final String LOGIN = "dhadmin";
    private static final String PASSWORD = "dhadmin_#911";
    private final HiveClient hiveClient;
    private final HiveMessageHandler<DeviceCommand> commandUpdatesHandler = new HiveMessageHandler<DeviceCommand>() {
        @Override
        public void handle(DeviceCommand command) {
            print("Command derived and proceed: {}", command);
        }
    };

    public RealDeviceExample(PrintStream out, String... args) throws ExampleException, HiveException {
        super(out, args);
        CommandLine commandLine = getCommandLine();
        hiveClient = HiveFactory.createClient(getServerUrl(), commandLine.hasOption(USE_SOCKETS), Example.HIVE_CONNECTION_EVENT_HANDLER);
    }

    public static void main(String... args) {
        try {
            Example clientExample = new RealDeviceExample(System.out, args);
            clientExample.run();
        } catch (HiveException | ExampleException | IOException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public Options makeOptionsSet() {
        return new Options();
    }

    @Override
    public void run() throws HiveException, ExampleException, IOException {
        try {
            hiveClient.authenticate(LOGIN, PASSWORD);
            HiveMessageHandler<DeviceNotification> notificationsHandler = new HiveMessageHandler<DeviceNotification>() {
                @Override
                public void handle(DeviceNotification notification) {
                    print("Notification received: {}" + notification);
                }
            };
            Timestamp serverTimestamp = hiveClient.getInfo().getServerTimestamp();
            SubscriptionFilter notificationSubscriptionFilter = new SubscriptionFilter(null, null, serverTimestamp);
            hiveClient.getNotificationsController().subscribeForNotifications(notificationSubscriptionFilter,
                    notificationsHandler);
            ScheduledExecutorService commandsExecutor = Executors.newSingleThreadScheduledExecutor();
            commandsExecutor.scheduleAtFixedRate(new CommandTask(), 3, 3, TimeUnit.SECONDS);
            Thread.currentThread().join(TimeUnit.MINUTES.toMillis(10));
            commandsExecutor.shutdownNow();
        } catch (InterruptedException e) {
            throw new ExampleException(e.getMessage(), e);
        } finally {
            hiveClient.close();
        }
    }

    private class CommandTask implements Runnable {
        private static final String LED_GREEN = "LED_G";
        private static final String LED_RED = "LED_R";
        private static final String LED_TYPE = "equipment";
        private static final String LED_STATE = "state";
        private static final String COMMAND = "UpdateLedState";
        private static final String uuid = "c73ccf23-8bf5-4c2c-b330-ead36f469d1a";
        private volatile boolean isGreen = false;
        private volatile boolean isRed = false;
        private volatile boolean isItGreenTurn = true;

        @Override
        public void run() {
            try {

                CommandsController cc = hiveClient.getCommandsController();
                NotificationsController nc = hiveClient.getNotificationsController();
                DeviceCommand command = new DeviceCommand();
                command.setCommand(COMMAND);
                JsonObject commandParams = new JsonObject();
                if (isItGreenTurn) {
                    commandParams.addProperty(LED_TYPE, LED_GREEN);
                    if (isGreen)
                        commandParams.addProperty(LED_STATE, 0);
                    else commandParams.addProperty(LED_STATE, 1);
                    isGreen = !isGreen;
                    isItGreenTurn = false;
                } else {
                    commandParams.addProperty(LED_TYPE, LED_RED);
                    if (isRed)
                        commandParams.addProperty(LED_STATE, 0);
                    else commandParams.addProperty(LED_STATE, 1);
                    isRed = !isRed;
                    isItGreenTurn = true;

                }
                command.setParameters(new JsonStringWrapper(commandParams.toString()));
                cc.insertCommand(uuid, command, commandUpdatesHandler);
                print("The command {} will be sent to all available devices", command.getCommand());

            } catch (HiveException e) {
                print(e.getMessage());
            }
        }
    }
}
