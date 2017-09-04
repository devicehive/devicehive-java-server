package com.devicehive.model.rpc;

/*
 * #%L
 * DeviceHive Common Module
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

import com.devicehive.model.DeviceCommand;
import com.devicehive.shim.api.Action;
import com.devicehive.shim.api.Body;

import java.util.Collection;
import java.util.Objects;

public class CommandSubscribeResponse extends Body {
    private Long subId;
    private Collection<DeviceCommand> commands;

    public CommandSubscribeResponse(Long subId, Collection<DeviceCommand> commands) {
        super(Action.COMMAND_SUBSCRIBE_RESPONSE);
        this.subId = subId;
        this.commands = commands;
    }

    public Long getSubId() {
        return subId;
    }

    public void setSubId(Long subId) {
        this.subId = subId;
    }

    public Collection<DeviceCommand> getCommands() {
        return commands;
    }

    public void setCommands(Collection<DeviceCommand> commands) {
        this.commands = commands;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommandSubscribeResponse)) return false;
        if (!super.equals(o)) return false;
        CommandSubscribeResponse that = (CommandSubscribeResponse) o;
        return Objects.equals(subId, that.subId) &&
                Objects.equals(commands, that.commands);

    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), subId, commands);
    }

    @Override
    public String toString() {
        return "CommandSubscribeResponse{" +
                "subId='" + subId + '\'' +
                ", commands=" + commands +
                '}';
    }
}
