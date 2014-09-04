package com.devicehive.messaging;

import com.devicehive.client.HiveClient;
import com.devicehive.client.HiveDevice;
import com.devicehive.client.HiveMessageHandler;
import com.devicehive.client.model.Device;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;
import com.devicehive.client.model.JsonStringWrapper;
import com.devicehive.client.model.exceptions.HiveException;
import com.devicehive.messaging.config.Constants;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Messager {
    private static final Logger logger = LoggerFactory.getLogger(Messager.class);
    private final HiveMessageHandler<DeviceCommand> commandsHandler = new HiveMessageHandler<DeviceCommand>() {
        @Override
        public void handle(DeviceCommand message) {
            commandsLock.lock();
            try {
                okCommands++;
                lostCommands--;
            } finally {
                commandsLock.unlock();
            }

        }
    };
    private final HiveMessageHandler<DeviceNotification> notificationsHandler =
            new HiveMessageHandler<DeviceNotification>() {
                @Override
                public void handle(DeviceNotification message) {
                    notificationsLock.lock();
                    try {
                        lostNotifications++;
                        okNotifications--;
                    } finally {
                        notificationsLock.unlock();
                    }
                }
            };
    private final DeviceNotification deviceNotification;
    private final DeviceCommand deviceCommand;
    private Lock commandsLock = new ReentrantLock();
    private Lock notificationsLock = new ReentrantLock();
    private long totalCommands = 0;
    private long lostCommands = 0;
    private long okCommands = 0;
    private long totalNotifications = 0;
    private long lostNotifications = 0;
    private long okNotifications = 0;
    private ScheduledExecutorService notificationsSES = Executors.newSingleThreadScheduledExecutor();
    private ScheduledExecutorService commandsSES = Executors.newSingleThreadScheduledExecutor();

    public Messager() {
        deviceNotification = new DeviceNotification();
        deviceNotification.setNotification("test notification");
        JsonObject params = new JsonObject();
        params.addProperty("param1", "value1");
        params.addProperty("param2", "value2");
        deviceNotification.setParameters(new JsonStringWrapper(params.toString()));

        deviceCommand = new DeviceCommand();
        deviceCommand.setCommand("test command");
        deviceCommand.setParameters(new JsonStringWrapper(params.toString()));
    }

    public void startSendNotifications(final List<HiveDevice> testDevices) {
        notificationsSES.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                sendNotifications(testDevices);
            }
        }, 0, Constants.TIME_INCREMENT, TimeUnit.SECONDS);
    }

    public void startSendCommands(final List<Device> testDevices, final List<HiveClient> testClients) {
        commandsSES.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                sendCommands(testDevices, testClients);
            }
        }, 0, Constants.TIME_INCREMENT, TimeUnit.SECONDS);
    }

    private void sendNotifications(List<HiveDevice> testDevices) {
        for (HiveDevice current : testDevices) {
            try {
                current.insertNotification(deviceNotification);
            } catch (HiveException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private void sendCommands(final List<Device> testDevices, final List<HiveClient> testClients) {

        for (HiveClient currentClient : testClients) {
            int deviceNum = ThreadLocalRandom.current().nextInt(0, testDevices.size() - 1);
            Device currentDevice = testDevices.get(deviceNum);
            try {
                currentClient.getCommandsController().insertCommand(currentDevice.getId(), deviceCommand, null);
            } catch (HiveException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public HiveMessageHandler<DeviceCommand> getCommandsHandler() {
        return commandsHandler;
    }

    public HiveMessageHandler<DeviceNotification> getNotificationsHandler() {
        return notificationsHandler;
    }
}
