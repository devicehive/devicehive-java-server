package com.devicehive.impl;

import com.devicehive.client.impl.util.CommandsHandler;
import com.devicehive.client.impl.util.NotificationsHandler;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;

import java.io.PrintStream;
import java.util.Date;

public final class HandlerImpl implements CommandsHandler, NotificationsHandler {
    private final PrintStream out;

    public HandlerImpl(PrintStream out) {
        this.out = out;
    }

    @Override
    public boolean handleCommandInsert(DeviceCommand command) {
        Date currentTime = new Date();
        out.println("Command received: " + command + " Current timestamp: " + currentTime.toString());
        return true;
    }

    @Override
    public boolean handleCommandUpdate(DeviceCommand command) {
        Date currentTime = new Date();
        out.println("Command updated: " + command + " Current timestamp: " + currentTime.toString());
        return true;
    }


    @Override
    public boolean handle(DeviceNotification notification) {
        Date currentTime = new Date();
        out.println("Notification received: " + notification + " Current timestamp: " + currentTime.toString());
        return true;
    }
}
