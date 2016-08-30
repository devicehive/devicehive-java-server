package com.devicehive.model.rpc;

import com.devicehive.model.DeviceCommand;
import com.devicehive.shim.api.Body;

import java.util.Collection;
import java.util.Objects;

public class CommandSubscribeResponse extends Body {
    private String subId;
    private Collection<DeviceCommand> commands;

    public CommandSubscribeResponse(String subId, Collection<DeviceCommand> commands) {
        super(Action.COMMAND_SUBSCRIBE_RESPONSE.name());
        this.subId = subId;
        this.commands = commands;
    }

    public String getSubId() {
        return subId;
    }

    public void setSubId(String subId) {
        this.subId = subId;
    }

    public Collection<DeviceCommand> getCommands() {
        return commands;
    }

    public void setCommands(Collection<DeviceCommand> commands) {
        this.commands = commands;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommandSubscribeResponse)) return false;
        if (!super.equals(o)) return false;
        CommandSubscribeResponse that = (CommandSubscribeResponse) o;
        return Objects.equals(subId, that.subId) &&
                Objects.equals(commands, that.commands);

    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), subId, commands);
    }

    @Override
    public String toString() {
        return "CommandSubscribeResponse{" +
                "subId='" + subId + '\'' +
                ", commands=" + commands +
                '}';
    }
}
