package com.devicehive.model.response;


import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.HiveEntity;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.COMMAND_LISTED;

public class CommandPollManyResponse implements HiveEntity {

    private static final long serialVersionUID = 3841209631676741432L;

    @SerializedName("command")
    @JsonPolicyDef(COMMAND_LISTED)
    private DeviceCommand command;

    @SerializedName("deviceGuid")
    @JsonPolicyDef(COMMAND_LISTED)
    private String guid;

    public CommandPollManyResponse(DeviceCommand command, String guid) {
        this.command = command;
        this.guid = guid;
    }

    public DeviceCommand getCommand() {
        return command;
    }

    public void setCommand(DeviceCommand command) {
        this.command = command;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public static List<CommandPollManyResponse> getList(List<DeviceCommand> commands) {
        List<CommandPollManyResponse> result = new ArrayList<>(commands.size());
        for (DeviceCommand command : commands) {
            result.add(new CommandPollManyResponse(command, command.getDevice().getGuid()));
        }
        return result;
    }

}
