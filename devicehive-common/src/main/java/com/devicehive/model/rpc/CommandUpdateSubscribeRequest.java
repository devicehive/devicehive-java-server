package com.devicehive.model.rpc;

import com.devicehive.shim.api.Body;

public class CommandUpdateSubscribeRequest extends Body {

    private long commandId;
    private String guid;

    public CommandUpdateSubscribeRequest(long commandId, String guid) {
        super(Action.COMMAND_UPDATE_SUBSCRIBE_REQUEST.name());
        this.commandId = commandId;
        this.guid = guid;
    }

    public long getCommandId() {
        return commandId;
    }

    public String getGuid() {
        return guid;
    }
}
