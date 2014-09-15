package com.devicehive.client.model;


import com.google.gson.annotations.SerializedName;

import com.devicehive.client.impl.json.strategies.JsonPolicyDef;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.COMMAND_TO_CLIENT;

public class CommandPollManyResponse implements HiveEntity {

    private static final long serialVersionUID = -4390548037685312874L;
    @SerializedName("notification")
    @JsonPolicyDef(COMMAND_TO_CLIENT)
    private DeviceCommand command;

    @SerializedName("deviceGuid")
    @JsonPolicyDef(COMMAND_TO_CLIENT)
    private String guid;

    public CommandPollManyResponse(DeviceCommand command, String guid) {
        this.command = command;
        this.guid = guid;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public DeviceCommand getCommand() {
        return command;
    }

    public void setCommand(DeviceCommand command) {
        this.command = command;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CommandPollManyResponse{");
        sb.append("command=").append(command);
        sb.append(", guid='").append(guid).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
