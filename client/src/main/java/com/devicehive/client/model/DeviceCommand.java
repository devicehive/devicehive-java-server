package com.devicehive.client.model;


import com.devicehive.client.impl.json.strategies.JsonPolicyDef;

import org.apache.commons.lang3.ObjectUtils;

import java.sql.Timestamp;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.COMMAND_FROM_CLIENT;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.COMMAND_LISTED;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.COMMAND_TO_CLIENT;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.COMMAND_TO_DEVICE;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.COMMAND_UPDATE_FROM_DEVICE;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.COMMAND_UPDATE_TO_CLIENT;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.POST_COMMAND_TO_DEVICE;

/**
 * Represents a device command, a unit of information sent to devices. For more details see <a
 * href="http://www.devicehive.com/restful#Reference/DeviceCommand"></a>
 */
public class DeviceCommand implements HiveMessage {

    private static final long serialVersionUID = -5147107009697358635L;
    @JsonPolicyDef(
        {COMMAND_TO_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private Long id;

    @JsonPolicyDef(
        {COMMAND_TO_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private Timestamp timestamp;

    @JsonPolicyDef({COMMAND_TO_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_LISTED})
    private Long userId;

    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT,
                    POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private String command;

    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT,
                    POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private JsonStringWrapper parameters;

    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_UPDATE_TO_CLIENT, COMMAND_LISTED})
    private Integer lifetime;

    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE, COMMAND_LISTED})
    private NullableWrapper<Integer> flags;

    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
                    POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private NullableWrapper<String> status;

    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
                    POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private NullableWrapper<JsonStringWrapper> result;


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

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public JsonStringWrapper getParameters() {
        return parameters;
    }

    public void setParameters(JsonStringWrapper parameters) {
        this.parameters = parameters;
    }

    public Integer getLifetime() {
        return lifetime;
    }

    public void setLifetime(Integer lifetime) {
        this.lifetime = lifetime;
    }

    public Integer getFlags() {
        return NullableWrapper.value(flags);
    }

    public void setFlags(Integer flags) {
        this.flags = NullableWrapper.create(flags);
    }

    public void removeFlags() {
        this.flags = null;
    }

    public String getStatus() {
        return NullableWrapper.value(status);
    }

    public void setStatus(String status) {
        this.status = NullableWrapper.create(status);
    }

    public void removeStatus() {
        this.status = null;
    }

    public JsonStringWrapper getResult() {
        return NullableWrapper.value(result);
    }

    public void setResult(JsonStringWrapper result) {
        this.result = NullableWrapper.create(result);
    }

    public void removeResult() {
        this.result = null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DeviceCommand{");
        sb.append("id=").append(id);
        sb.append(", timestamp=").append(timestamp);
        sb.append(", userId=").append(userId);
        sb.append(", command='").append(command).append('\'');
        sb.append(", parameters=").append(parameters);
        sb.append(", lifetime=").append(lifetime);
        sb.append(", flags=").append(flags);
        sb.append(", status=").append(status);
        sb.append(", result=").append(result);
        sb.append('}');
        return sb.toString();
    }
}
