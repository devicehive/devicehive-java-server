package com.devicehive.model.view;


import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.NullableWrapper;
import com.devicehive.model.domain.DeviceCommand;
import com.devicehive.model.domain.User;

import java.sql.Timestamp;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class DeviceCommandView implements HiveEntity {

    private static final long serialVersionUID = -5147107009697358635L;
    @JsonPolicyDef({COMMAND_TO_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT,
            POST_COMMAND_TO_DEVICE,
            COMMAND_LISTED})
    private Long id;
    @JsonPolicyDef(
            {COMMAND_TO_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT,
                    POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private Timestamp timestamp;
    @JsonPolicyDef({COMMAND_TO_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT,
            COMMAND_LISTED})
    private Long userId;
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT,
            COMMAND_UPDATE_FROM_DEVICE, POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private NullableWrapper<String> command;
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT,
            COMMAND_UPDATE_FROM_DEVICE, POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private NullableWrapper<JsonStringWrapper> parameters;
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE, COMMAND_LISTED})
    private NullableWrapper<Integer> lifetime;
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE, COMMAND_LISTED})
    private NullableWrapper<Integer> flags;
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT,
            COMMAND_UPDATE_FROM_DEVICE, POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private NullableWrapper<String> status;
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT,
            COMMAND_UPDATE_FROM_DEVICE, POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private NullableWrapper<JsonStringWrapper> result;
    @JsonPolicyDef({COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT})
    private DeviceSubscriptionView device;

    public DeviceCommandView() {
    }

    public DeviceCommandView(DeviceCommand deviceCommand) {
        convertFrom(deviceCommand);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public NullableWrapper<String> getCommand() {
        return command;
    }

    public void setCommand(NullableWrapper<String> command) {
        this.command = command;
    }

    public NullableWrapper<JsonStringWrapper> getParameters() {
        return parameters;
    }

    public void setParameters(NullableWrapper<JsonStringWrapper> parameters) {
        this.parameters = parameters;
    }

    public NullableWrapper<Integer> getLifetime() {
        return lifetime;
    }

    public void setLifetime(NullableWrapper<Integer> lifetime) {
        this.lifetime = lifetime;
    }

    public NullableWrapper<Integer> getFlags() {
        return flags;
    }

    public void setFlags(NullableWrapper<Integer> flags) {
        this.flags = flags;
    }

    public NullableWrapper<String> getStatus() {
        return status;
    }

    public void setStatus(NullableWrapper<String> status) {
        this.status = status;
    }

    public NullableWrapper<JsonStringWrapper> getResult() {
        return result;
    }

    public void setResult(NullableWrapper<JsonStringWrapper> result) {
        this.result = result;
    }

    public DeviceSubscriptionView getDevice() {
        return device;
    }

    public void setDevice(DeviceSubscriptionView device) {
        this.device = device;
    }

    public DeviceCommand convertTo() {
        DeviceCommand deviceCommand = new DeviceCommand();
        deviceCommand.setId(id);
        deviceCommand.setTimestamp(timestamp);
        User user = new User();   //todo
        user.setId(userId);
        deviceCommand.setUser(user);
        if (command != null) {
            deviceCommand.setCommand(command.getValue());
        }
        if (parameters != null) {
            deviceCommand.setParameters(parameters.getValue());
        }
        if (lifetime != null) {
            deviceCommand.setLifetime(lifetime.getValue());
        }
        if (flags != null) {
            deviceCommand.setFlags(flags.getValue());
        }
        if (status != null) {
            deviceCommand.setStatus(status.getValue());
        }
        if (result != null) {
            deviceCommand.setResult(result.getValue());
        }
        return deviceCommand;
    }

    public void convertFrom(DeviceCommand deviceCommand) {
        if (deviceCommand == null) {
            return;
        }
        id = deviceCommand.getId();
        timestamp = deviceCommand.getTimestamp();
        if (deviceCommand.getUser() != null) {
            userId = deviceCommand.getUser().getId();
        }
        command = new NullableWrapper<>(deviceCommand.getCommand());
        parameters = new NullableWrapper<>(deviceCommand.getParameters());
        lifetime = new NullableWrapper<>(deviceCommand.getLifetime());
        flags = new NullableWrapper<>(deviceCommand.getFlags());
        status = new NullableWrapper<>(deviceCommand.getStatus());
        result = new NullableWrapper<>(deviceCommand.getResult());
        device = new DeviceSubscriptionView(deviceCommand.getDevice().getGuid(), deviceCommand.getDevice().getId());

    }
}
