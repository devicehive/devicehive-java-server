package com.devicehive.model.eventbus.events;

import com.devicehive.model.DeviceCommand;
import com.devicehive.model.eventbus.Subscription;
import com.devicehive.model.rpc.Action;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

public class CommandUpdateEvent extends Event {

    private DeviceCommand command;

    public CommandUpdateEvent(DeviceCommand command) {
        super(Action.COMMAND_UPDATE_EVENT.name());
        this.command = command;
    }

    @Override
    public Collection<Subscription> getApplicableSubscriptions() {
        Subscription device = new Subscription(Action.COMMAND_UPDATE_EVENT.name(), command.getId().toString());
        Subscription deviceWithName = new Subscription(
                Action.COMMAND_UPDATE_EVENT.name(), command.getId().toString(), command.getCommand());
        return Arrays.asList(device, deviceWithName);
    }

    public DeviceCommand getDeviceCommand() {
        return command;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommandUpdateEvent)) return false;
        if (!super.equals(o)) return false;
        CommandUpdateEvent that = (CommandUpdateEvent) o;
        return Objects.equals(command, that.command);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), command);
    }

    @Override
    public String toString() {
        return "CommandUpdateEvent{" +
                "command=" + command +
                '}';
    }
}
