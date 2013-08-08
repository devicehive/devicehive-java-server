package com.devicehive.model.response;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.DeviceCommand;
import com.google.gson.annotations.SerializedName;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

/**
 * Created with IntelliJ IDEA.
 * User: nloboda
 * Date: 07.08.13
 * Time: 17:42
 * To change this template use File | Settings | File Templates.
 */
public class DeviceCommandWithUserId extends DeviceCommand {

    @SerializedName("userId")
    @JsonPolicyDef({COMMAND_TO_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, POST_COMMAND_TO_DEVICE})
    private Long userId;

    public static DeviceCommandWithUserId fromDeviceCommand(DeviceCommand dc) {

        DeviceCommandWithUserId dcwi = new DeviceCommandWithUserId();
        dcwi.setId(dc.getId());
        dcwi.setUserId(dc.getUserId());
        dcwi.setUser(dc.getUser());
        dcwi.setTimestamp(dc.getTimestamp());
        dcwi.setCommand(dc.getCommand());
        dcwi.setParameters(dc.getParameters());
        dcwi.setLifetime(dc.getLifetime());
        dcwi.setFlags(dc.getFlags());
        dcwi.setStatus(dc.getStatus());
        dcwi.setResult(dc.getResult());
        dcwi.setDevice(dc.getDevice());
        dcwi.setEntityVersion(dc.getEntityVersion());
        dcwi.setUserId(dc.getUserId());
        return dcwi;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
