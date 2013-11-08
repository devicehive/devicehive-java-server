package com.devicehive.client.example;


import com.devicehive.client.api.HiveDeviceGateway;
import com.devicehive.client.model.*;
import com.devicehive.client.model.exceptions.HiveException;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DeviceGatewayRestExample {

    private static Logger logger = LoggerFactory.getLogger(SingleHiveDeviceRestExample.class);
    private static ScheduledExecutorService commandsUpdater = Executors.newSingleThreadScheduledExecutor();

    /**
     * example's main method
     *
     * @param args args[0] - REST server URI
     *             args[1] - Web socket server URI
     */
    public static void main(String... args) {
        URI restUri = URI.create(args[0]);
        URI websocketUri = URI.create(args[1]);
        final HiveDeviceGateway hdg = new HiveDeviceGateway(restUri, websocketUri, Transport.PREFER_REST);
        try {
            //save device
            final Device deviceToSave = createDeviceToSave();
            hdg.saveDevice(deviceToSave.getId(), deviceToSave.getKey(), deviceToSave);
            logger.debug("device saved");

            //get device
            Device savedDevice = hdg.getDevice(deviceToSave.getId(), deviceToSave.getKey());
            logger.debug("saved device: id {}, name {}, status {}, data {}, device class id {}, " +
                    "device class name {}, device class version {}", savedDevice.getId(),
                    savedDevice.getName(), savedDevice.getStatus(), savedDevice.getData(),
                    savedDevice.getDeviceClass().getId(), savedDevice.getDeviceClass().getName(),
                    savedDevice.getDeviceClass().getVersion());

            //update device
            deviceToSave.setStatus("updated example status");
            hdg.saveDevice(deviceToSave.getId(), deviceToSave.getKey(), deviceToSave);
            logger.debug("device updated");

            //get device
            Device updatedDevice = hdg.getDevice(deviceToSave.getId(), deviceToSave.getKey());
            logger.debug("updated device: id {}, name {}, status {}, data {}, device class id {}, " +
                    "device class name {}, device class version {}", updatedDevice.getId(),
                    updatedDevice.getName(), updatedDevice.getStatus(), updatedDevice.getData(),
                    updatedDevice.getDeviceClass().getId(), updatedDevice.getDeviceClass().getName(),
                    updatedDevice.getDeviceClass().getVersion());

            //subscribe for commands
            try {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
                Date startDate = formatter.parse("2013-10-11 13:12:00");
                hdg.subscribeForCommands(deviceToSave.getId(), deviceToSave.getKey(),
                        new Timestamp(startDate.getTime()));
                logger.debug("device subscribed for commands");
            } catch (ParseException e) {
                logger.error(e.getMessage(), e);
            }

            //update commands
            commandsUpdater.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    while (!Thread.currentThread().isInterrupted()) {
                        updateCommands(hdg, deviceToSave.getId(), deviceToSave.getKey());
                    }
                }
            }, 0, 1, TimeUnit.SECONDS);

           //notification insert
            hdg.insertNotification(deviceToSave.getId(), deviceToSave.getKey(), createNotification());

            try {
                Thread.currentThread().join(5_000);
                hdg.unsubscribeFromCommands(deviceToSave.getId(), deviceToSave.getKey());
                Thread.currentThread().join(5_000);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        } catch (HiveException e) {
            logger.error(e.getMessage(), e);
        } finally {
            killUpdater();
            try {
                hdg.close();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private static Device createDeviceToSave() {
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

    private static void updateCommands(HiveDeviceGateway hdg, String deviceId, String deviceKey) {
        Queue<Pair<String, DeviceCommand>> commandsQueue = hdg.getCommandsQueue();
        Iterator<Pair<String, DeviceCommand>> commandIterator = commandsQueue.iterator();
        while (commandIterator.hasNext()) {
            Pair<String, DeviceCommand> pair = commandIterator.next();
            if (pair.getLeft().equals(deviceId)) {
                DeviceCommand command = pair.getRight();
                command.setStatus("procceed");
                hdg.updateCommand(deviceId, deviceKey, command);
                commandIterator.remove();
                logger.debug("command with id {} is updated", command.getId());
            }

        }
    }

    private static DeviceNotification createNotification() {
        DeviceNotification notification = new DeviceNotification();
        notification.setNotification("example notification");
        notification.setParameters(new JsonStringWrapper("{\"params\": example_param}"));
        return notification;
    }

    private static void killUpdater() {
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
