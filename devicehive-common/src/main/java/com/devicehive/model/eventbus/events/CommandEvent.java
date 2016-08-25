package com.devicehive.model.eventbus.events;

import com.devicehive.model.DeviceCommand;
import com.devicehive.model.eventbus.Subscription;
import com.devicehive.model.rpc.Action;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

public class CommandEvent extends Event {

    private DeviceCommand command;

    public CommandEvent(DeviceCommand command) {
        super(Action.COMMAND.name());
        this.command = command;
    }

    public DeviceCommand getCommand() {
        return command;
    }

    public void setCommand(DeviceCommand command) {
        this.command = command;
    }

    @Override
    public Collection<Subscription> getApplicableSubscriptions() {
        Subscription device = new Subscription(Action.COMMAND.name(), command.getDeviceGuid());
        Subscription deviceWithName = new Subscription(Action.COMMAND.name(), command.getDeviceGuid(), command.getCommand());
        return Arrays.asList(device, deviceWithName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommandEvent)) return false;
        if (!super.equals(o)) return false;
        CommandEvent that = (CommandEvent) o;
        return Objects.equals(command, that.command);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), command);
    }

    @Override
    public String toString() {
        return "CommandEvent{" +
                "command=" + command +
                '}';
    }
}
