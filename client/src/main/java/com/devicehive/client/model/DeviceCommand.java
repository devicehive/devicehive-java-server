package com.devicehive.client.model;


import com.devicehive.client.json.strategies.JsonPolicyDef;
import org.apache.commons.lang3.ObjectUtils;

import java.sql.Timestamp;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.*;

public class DeviceCommand implements HiveEntity {

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
    private DeviceSubscription device;

    public DeviceCommand() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Timestamp getTimestamp() {
        return ObjectUtils.cloneIfPossible(timestamp);
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = ObjectUtils.cloneIfPossible(timestamp);
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

    public DeviceSubscription getDevice() {
        return device;
    }

    public void setDevice(DeviceSubscription device) {
        this.device = device;
    }
}
