package com.devicehive.model.rpc;

import com.devicehive.shim.api.Body;

public class CommandUpdateSubscribeRequest extends Body {

    private long commamdId;
    private String guid;

    public CommandUpdateSubscribeRequest(long commamdId, String guid) {
        super(Action.COMMAND_UPDATE_SUBSCRIBE_REQUEST.name());
        this.commamdId = commamdId;
    }

    public long getCommamdId() {
        return commamdId;
    }

    public String getGuid() {
        return guid;
    }
}
