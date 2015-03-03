package com.devicehive.model.response;


import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.DeviceCommandMessage;
import com.devicehive.model.HiveEntity;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.COMMAND_LISTED;

public class CommandPollManyResponse implements HiveEntity {

    private static final long serialVersionUID = 3841209631676741432L;

    @SerializedName("command")
    @JsonPolicyDef(COMMAND_LISTED)
    private DeviceCommandMessage command;

    @SerializedName("deviceGuid")
    @JsonPolicyDef(COMMAND_LISTED)
    private String guid;

    public CommandPollManyResponse(DeviceCommandMessage command, String guid) {
        this.command = command;
        this.guid = guid;
    }

    public DeviceCommandMessage getCommand() {
        return command;
    }

    public void setCommand(DeviceCommandMessage command) {
        this.command = command;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public static List<CommandPollManyResponse> getList(List<DeviceCommandMessage> commands) {
        List<CommandPollManyResponse> result = new ArrayList<>(commands.size());
        for (DeviceCommandMessage command : commands) {
            result.add(new CommandPollManyResponse(command, command.getDeviceGuid()));
        }
        return result;
    }

}
