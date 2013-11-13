package com.devicehive.client.example.device;


import com.devicehive.client.api.SingleHiveDevice;
import com.devicehive.client.model.*;
import com.devicehive.client.model.exceptions.HiveException;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class SingleHiveDeviceExample {
    private static Logger logger = LoggerFactory.getLogger(SingleHiveDeviceExample.class);
    private static ScheduledExecutorService commandsUpdater = Executors.newSingleThreadScheduledExecutor();

    public void example(final SingleHiveDevice shd) {
        try {
            //save device
            Device deviceToSave = createDeviceToSave();
            shd.saveDevice(deviceToSave);
            logger.debug("device saved");

            //authenticate device
            shd.authenticate(deviceToSave.getId(), deviceToSave.getKey());
            logger.debug("device authenticated");

            //get device
            Device savedDevice = shd.getDevice();
            logger.debug("saved device: id {}, name {}, status {}, data {}, device class id {}, " +
                    "device class name {}, device class version {}", savedDevice.getId(),
                    savedDevice.getName(), savedDevice.getStatus(), savedDevice.getData(),
                    savedDevice.getDeviceClass().getId(), savedDevice.getDeviceClass().getName(),
                    savedDevice.getDeviceClass().getVersion());

            //update device
            deviceToSave.setStatus("updated example status");
            shd.saveDevice(deviceToSave);
            logger.debug("device updated");

            //get device
            Device updatedDevice = shd.getDevice();
            logger.debug("updated device: id {}, name {}, status {}, data {}, device class id {}, " +
                    "device class name {}, device class version {}", updatedDevice.getId(),
                    updatedDevice.getName(), updatedDevice.getStatus(), updatedDevice.getData(),
                    updatedDevice.getDeviceClass().getId(), updatedDevice.getDeviceClass().getName(),
                    updatedDevice.getDeviceClass().getVersion());

            //subscribe for commands
            try {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
                Date startDate = formatter.parse("2013-10-11 13:12:00");
                shd.subscribeForCommands(new Timestamp(startDate.getTime()));
                logger.debug("device subscribed for commands");
            } catch (ParseException e) {
                logger.error(e.getMessage(), e);
            }

            //update commands
            commandsUpdater.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    while (!Thread.currentThread().isInterrupted()) {
                        updateCommands(shd);
                    }
                }
            }, 0, 1, TimeUnit.SECONDS);

            //notification insert
            shd.insertNotification(createNotification());

            try {
                Thread.currentThread().join(5_000);
                shd.unsubscribeFromCommands(null);
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

    private void updateCommands(SingleHiveDevice shd) {
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
}
