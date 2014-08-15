package com.devicehive.messaging;

import com.devicehive.client.HiveMessageHandler;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Messager {

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

    private Lock commandsLock = new ReentrantLock();
    private Lock notificationsLock = new ReentrantLock();
    private long totalCommands = 0;
    private long lostCommands = 0;
    private long okCommands = 0;
    private long totalNotifications = 0;
    private long lostNotifications = 0;
    private long okNotifications = 0;

    public void sendNotifications(){

    }

    public HiveMessageHandler<DeviceCommand> getCommandsHandler() {
        return commandsHandler;
    }

    public HiveMessageHandler<DeviceNotification> getNotificationsHandler() {
        return notificationsHandler;
    }
}
