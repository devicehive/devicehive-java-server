package com.devicehive.model.wrappers;

/*
 * #%L
 * DeviceHive Java Server Common business logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.JsonStringWrapper;
import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.Optional;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

/**
 * Created by tatyana on 2/12/15.
 */
public class DeviceCommandWrapper implements HiveEntity {
    private static final long serialVersionUID = 1179387574631106725L;

    @SerializedName("command")
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
            POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private String command;

    @SerializedName("timestamp")
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
            POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private Date timestamp;

    @SerializedName("parameters")
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
            POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private JsonStringWrapper parameters;

    @SerializedName("lifetime")
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
            COMMAND_LISTED})
    private Integer lifetime;

    @SerializedName("status")
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
            POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private String status;

    @SerializedName("result")
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
            POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private JsonStringWrapper result;

    public Optional<String> getCommand() {
        return Optional.ofNullable(command);
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public Optional<Date> getTimestamp() {
        return Optional.ofNullable(timestamp);
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Optional<JsonStringWrapper> getParameters() {
        return Optional.ofNullable(parameters);
    }

    public void setParameters(JsonStringWrapper parameters) {
        this.parameters = parameters;
    }

    public Optional<Integer> getLifetime() {
        return Optional.ofNullable(lifetime);
    }

    public void setLifetime(Integer lifetime) {
        this.lifetime = lifetime;
    }

    public Optional<String> getStatus() {
        return Optional.ofNullable(status);
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Optional<JsonStringWrapper> getResult() {
        return Optional.ofNullable(result);
    }

    public void setResult(JsonStringWrapper result) {
        this.result = result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeviceCommandWrapper that = (DeviceCommandWrapper) o;

        if (command != null ? !command.equals(that.command) : that.command != null) return false;
        if (lifetime != null ? !lifetime.equals(that.lifetime) : that.lifetime != null) return false;
        if (parameters != null ? !parameters.equals(that.parameters) : that.parameters != null) return false;
        if (result != null ? !result.equals(that.result) : that.result != null) return false;
        if (status != null ? !status.equals(that.status) : that.status != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result1 = command != null ? command.hashCode() : 0;
        result1 = 31 * result1 + (parameters != null ? parameters.hashCode() : 0);
        result1 = 31 * result1 + (lifetime != null ? lifetime.hashCode() : 0);
        result1 = 31 * result1 + (status != null ? status.hashCode() : 0);
        result1 = 31 * result1 + (result != null ? result.hashCode() : 0);
        return result1;
    }

    @Override
    public String toString() {
        return "DeviceCommandWrapper{" +
                "command='" + command + '\'' +
                ", parameters=" + parameters +
                ", lifetime=" + lifetime +
                ", status='" + status + '\'' +
                ", result=" + result +
                '}';
    }
}
