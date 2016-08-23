package com.devicehive.model.rpc;

import com.devicehive.model.DeviceCommand;
import com.devicehive.shim.api.Body;

import java.util.Collections;
import java.util.List;

public class CommandSearchResponse extends Body {

    private List<DeviceCommand> commands;

    public CommandSearchResponse() {
        super(Action.COMMAND_SEARCH_RESPONSE.name());
        this.commands = Collections.emptyList();
    }

    public List<DeviceCommand> getCommands() {
        return commands;
    }

    public void setCommands(List<DeviceCommand> commands) {
        this.commands = commands;
    }
}
