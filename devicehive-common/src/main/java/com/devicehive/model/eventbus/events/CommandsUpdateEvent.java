package com.devicehive.model.eventbus.events;

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
import com.devicehive.model.eventbus.Subscription;
import com.devicehive.model.rpc.Action;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

public class CommandsUpdateEvent extends Event {

    private DeviceCommand command;

    public CommandsUpdateEvent(DeviceCommand command) {
        super(Action.COMMANDS_UPDATE_EVENT.name());
        this.command = command;
    }

    @Override
    public Collection<Subscription> getApplicableSubscriptions() {
        Subscription device = new Subscription(Action.COMMANDS_UPDATE_EVENT.name(), command.getDeviceId());
        Subscription deviceWithName = new Subscription(
                Action.COMMANDS_UPDATE_EVENT.name(), command.getDeviceId(), command.getCommand());
        
        return Arrays.asList(device, deviceWithName);
    }

    public DeviceCommand getDeviceCommand() {
        return command;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommandsUpdateEvent)) return false;
        if (!super.equals(o)) return false;
        CommandsUpdateEvent that = (CommandsUpdateEvent) o;
        return Objects.equals(command, that.command);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), command);
    }

    @Override
    public String toString() {
        return "CommandsUpdateEvent{" +
                "command=" + command +
                '}';
    }
}
