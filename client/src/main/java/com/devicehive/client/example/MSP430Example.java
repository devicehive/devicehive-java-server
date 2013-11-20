package com.devicehive.client.example;

import com.devicehive.client.api.client.Client;
import com.devicehive.client.api.client.CommandsController;
import com.devicehive.client.api.client.NotificationsController;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.JsonStringWrapper;
import com.devicehive.client.model.Transport;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * MSP430 LaunchPad example
 * <p/>
 * subscribe for notifications,
 * send commands
 * receive notifications
 * Every 200 ms green or red diode changes it's state
 */
public class MSP430Example {

    private static final Logger logger = LoggerFactory.getLogger(MSP430Example.class);
    private static final String guid = "c73ccf23-8bf5-4c2c-b330-ead36f469d1a";
    private final HelpFormatter HELP_FORMATTER = new HelpFormatter();
    private final DeviceCommand command = new DeviceCommand();
    private final ReentrantLock lock = new ReentrantLock();
    private Client userClient;
    private ScheduledExecutorService notificationsMonitor = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean greenState = false;
    private volatile boolean redState = false;
    private volatile int i = 0;
    private ScheduledExecutorService commandsInsertService = Executors.newSingleThreadScheduledExecutor();
    private ScheduledExecutorService commandsUpdatesService = Executors.newSingleThreadScheduledExecutor();
    private PrintStream out;
    private Options options = new Options();
    private URI rest;
    private URI webSocket;
    private Transport transport;
    private long interval;
    private boolean isParseable = true;

    public MSP430Example(PrintStream out) {
        this.out = out;
    }

    /**
     * example's main method
     *
     * @param args args[0] - REST server URI
     *             args[1] - Web socket server URI
     */
    public static void main(String... args) {
        MSP430Example example = new MSP430Example(System.out);
        example.run(args);
    }

    public void run(String... args) {
        initOptions();
        parseArguments(args);
        if (isParseable)
            try {
                init();
                subscribeForNotifications();
                commandInsertServiceStart();
                Thread.currentThread().join(15_000);
            } catch (Exception e) {
                logger.debug(e.getMessage(), e);
            } finally {
                close();
            }
    }

    private void init() {
        userClient = new Client(rest, webSocket, transport);
        userClient.authenticate("dhadmin", "dhadmin_#911");
        command.setCommand("UpdateLedState");
    }

    private void close() {
        try {
            if (userClient != null)
                userClient.close();
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        } finally {
            notificationsMonitor.shutdown();
            commandsInsertService.shutdown();
            commandsUpdatesService.shutdown();
        }
    }

    private void commandInsertServiceStart() {
        final CommandsController controller = userClient.getCommandsController();
        commandsInsertService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    if (i % 2 == 0) {
                        if (greenState) {
                            command.setParameters(new JsonStringWrapper("{\"equipment\":\"LED_G\",\"state\":1}"));
                            greenState = !greenState;
                        } else {
                            command.setParameters(new JsonStringWrapper("{\"equipment\":\"LED_G\",\"state\":0}"));
                            greenState = !greenState;
                        }
                    } else {
                        if (redState) {
                            command.setParameters(new JsonStringWrapper("{\"equipment\":\"LED_R\",\"state\":1}"));
                            redState = !redState;
                        } else {
                            command.setParameters(new JsonStringWrapper("{\"equipment\":\"LED_R\",\"state\":0}"));
                            redState = !redState;
                        }
                    }
                    i++;
                    controller.insertCommand(guid, command);
                } finally {
                    lock.unlock();
                }
            }
        }, 0, interval / 2, TimeUnit.MILLISECONDS);
    }

    private void commandUpdateServiceStart() {
        final CommandsController controller = userClient.getCommandsController();
        commandsUpdatesService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Queue<DeviceCommand> queue = controller.getCommandUpdatesQueue();
                while (!queue.isEmpty()) {
                    DeviceCommand command = queue.poll();
                    out.println("command updated: " + command.getId());
                }
            }
        }, 0, interval / 2, TimeUnit.MILLISECONDS);
    }

    private void subscribeForNotifications() {
        final NotificationsController controller = userClient.getNotificationsController();
        controller.subscribeForNotifications(null, null, guid);
        notificationsMonitor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Queue<Pair<String, DeviceNotification>> queue = controller.getNotificationsQueue();
                while (!queue.isEmpty()) {
                    Pair<String, DeviceNotification> pair = queue.poll();
                    out.println("guid: " + pair.getLeft() + " notification: " + pair.getRight()
                            .getNotification());
                }
            }
        }, 0, interval / 2, TimeUnit.MILLISECONDS);
    }

    public void printUsage() {
        HELP_FORMATTER.printHelp("MSP430Example", options);
    }

    public void parseArguments(String... args) {
        CommandLineParser parser = new BasicParser();
        try {
            CommandLine cmdLine = parser.parse(options, args);
            rest = URI.create(cmdLine.getOptionValue("rest"));
            webSocket = URI.create(cmdLine.getOptionValue("ws"));
            transport = cmdLine.hasOption("use_sockets") ? Transport.PREFER_WEBSOCKET : Transport.PREFER_REST;
            long defaultInterval = 200;
            interval = cmdLine.hasOption("interval")
                    ? Long.parseLong(cmdLine.getOptionValue("interval"))
                    : defaultInterval;
        } catch (ParseException e) {
            logger.error("unable to parse command line arguments!");
            printUsage();
            isParseable = false;
        }
    }

    public void initOptions() {
        Option restUrl = OptionBuilder.hasArg()
                .withArgName("rest")
                .withDescription("REST service URL")
                .isRequired(true)
                .create("rest");
        Option wsURL = OptionBuilder.hasArg()
                .withArgName("ws")
                .withDescription("WebSocket service URL")
                .isRequired(true).create("ws");
        Option timeInterval = OptionBuilder.hasArg()
                .withArgName("interval")
                .withDescription("time interval in ms for sending commands to device")
                .create("interval");
        Option transport = OptionBuilder.hasArg(false)
                .withDescription("if set use sockets")
                .create("use_sockets");
        options.addOption(restUrl);
        options.addOption(wsURL);
        options.addOption(timeInterval);
        options.addOption(transport);
    }
}
