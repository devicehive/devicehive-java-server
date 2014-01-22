package com.devicehive.client.example.device;


import com.devicehive.client.HiveDevice;
import com.devicehive.client.impl.HiveDeviceRestImpl;
import com.devicehive.client.model.*;
import com.devicehive.client.model.exceptions.HiveException;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SingleHiveDeviceExample {
    private static Logger logger = LoggerFactory.getLogger(SingleHiveDeviceExample.class);
    private final HelpFormatter HELP_FORMATTER = new HelpFormatter();
    private ScheduledExecutorService commandsUpdater = Executors.newSingleThreadScheduledExecutor();
    private Options options = new Options();
    private URI rest;
    private boolean isParseable = true;
    private Transport transport;
    private Device deviceToSave;

    public static void main(String... args) {
        SingleHiveDeviceExample example = new SingleHiveDeviceExample();
        example.run(args);
    }

    private void example(HiveDeviceRestImpl shd) {
        try {
            //save device
            saveDeviceExample(shd);

            //authenticate device
            authenticationExample(shd);

            //get device
            getDeviceExample(shd);

            //update device
            updateDeviceExample(shd);

            //subscribe for commands
            commandSubscriptionExample(shd);

            //update commands
            commandUpdatesExample(shd);

            //notification insert
            shd.insertNotification(createNotification());

            try {
                Thread.currentThread().join(5_000);
                shd.unsubscribeFromCommands();
                Thread.currentThread().join(5_000);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        } catch (HiveException e) {
            logger.error(e.getMessage(), e);
        } finally {
            killUpdater();
            try {
                shd.close();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private void saveDeviceExample(final HiveDevice shd) {
        deviceToSave = createDeviceToSave();
        shd.registerDevice(deviceToSave);
        logger.info("device saved");
    }

    private void authenticationExample(final HiveDevice shd) {
        shd.authenticate(deviceToSave.getId(), deviceToSave.getKey());
        logger.info("device authenticated");
    }

    private void getDeviceExample(final HiveDevice shd) {
        Device savedDevice = shd.getDevice();
        logger.info("saved device: id {}, name {}, status {}, data {}, device class id {}, " +
                "device class name {}, device class version {}", savedDevice.getId(),
                savedDevice.getName(), savedDevice.getStatus(), savedDevice.getData(),
                savedDevice.getDeviceClass().getId(), savedDevice.getDeviceClass().getName(),
                savedDevice.getDeviceClass().getVersion());
    }

    private void updateDeviceExample(final HiveDevice shd) {
        deviceToSave.setStatus("updated example status");
        shd.registerDevice(deviceToSave);
        logger.debug("device updated");
        Device updatedDevice = shd.getDevice();
        logger.info("updated device: id {}, name {}, status {}, data {}, device class id {}, " +
                "device class name {}, device class version {}", updatedDevice.getId(),
                updatedDevice.getName(), updatedDevice.getStatus(), updatedDevice.getData(),
                updatedDevice.getDeviceClass().getId(), updatedDevice.getDeviceClass().getName(),
                updatedDevice.getDeviceClass().getVersion());
    }

    private void commandSubscriptionExample(final HiveDevice shd) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
            Date startDate = formatter.parse("2013-10-11 13:12:00");
            shd.subscribeForCommands(new Timestamp(startDate.getTime()));
            logger.info("device subscribed for commands");
        } catch (java.text.ParseException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void commandUpdatesExample(final HiveDevice shd) {
        commandsUpdater.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    updateCommands(shd);
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private Device createDeviceToSave() {
        Device device = new Device();
        device.setId(UUID.randomUUID().toString());
        device.setKey(UUID.randomUUID().toString());
        device.setStatus("new device example status");
        device.setName("example device");
        DeviceClass deviceClass = new DeviceClass();
        deviceClass.setName("example device class name");
        deviceClass.setVersion("v1");
        deviceClass.setPermanent(true);
        Set<Equipment> equipmentSet = new HashSet<>();
        Equipment equipment = new Equipment();
        equipment.setName("example equipment name");
        equipment.setCode(UUID.randomUUID().toString());
        equipment.setType("example");
        equipmentSet.add(equipment);
        deviceClass.setEquipment(equipmentSet);
        device.setDeviceClass(deviceClass);
        Network network = new Network();
        network.setId(1L);
        network.setName("VirtualLed Sample Network");
        device.setNetwork(network);
        return device;
    }

    private void updateCommands(HiveDevice shd) {
        Queue<Pair<String, DeviceCommand>> commandsQueue = shd.getCommandsQueue();
        while (!commandsQueue.isEmpty()) {
            DeviceCommand command = commandsQueue.poll().getRight();
            command.setStatus("procceed");
            shd.updateCommand(command);
            logger.debug("command with id {} is updated", command.getId());
        }
    }

    private DeviceNotification createNotification() {
        DeviceNotification notification = new DeviceNotification();
        notification.setNotification("example notification");
        notification.setParameters(new JsonStringWrapper("{\"params\": example_param}"));
        return notification;
    }

    private void killUpdater() {
        commandsUpdater.shutdown();
        try {
            if (!commandsUpdater.awaitTermination(5, TimeUnit.SECONDS)) {
                commandsUpdater.shutdownNow();
                if (!commandsUpdater.awaitTermination(5, TimeUnit.SECONDS))
                    logger.warn("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            logger.warn(ie.getMessage(), ie);
            commandsUpdater.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void printUsage() {
        HELP_FORMATTER.printHelp("SingleHiveDeviceExample", options);
    }

    private void parseArguments(String... args) {
        CommandLineParser parser = new BasicParser();
        try {
            CommandLine cmdLine = parser.parse(options, args);
            rest = URI.create(cmdLine.getOptionValue("rest"));
            transport = cmdLine.hasOption("use_sockets") ? Transport.PREFER_WEBSOCKET : Transport.REST_ONLY;
        } catch (org.apache.commons.cli.ParseException e) {
            logger.error("unable to parse command line arguments!");
            printUsage();
            isParseable = false;
        }
    }

    private void initOptions() {
        Option restUrl = OptionBuilder.hasArg()
                .withArgName("rest")
                .withDescription("REST service URL")
                .isRequired(true)
                .create("rest");
        Option transport = OptionBuilder.hasArg(false)
                .withDescription("if set use sockets")
                .create("use_sockets");
        options.addOption(restUrl);
        options.addOption(transport);
    }

    public void run(String... args) {
        initOptions();
        parseArguments(args);
        if (isParseable) {
            try {
                final HiveDeviceRestImpl shd = new HiveDeviceRestImpl(rest, transport);
                example(shd);
            } catch (Exception e) {
                logger.debug(e.getMessage(), e);
            }
        }
    }

}
