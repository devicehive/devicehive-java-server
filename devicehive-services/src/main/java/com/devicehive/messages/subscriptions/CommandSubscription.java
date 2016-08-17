package com.devicehive.messages.subscriptions;

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


import com.devicehive.auth.HivePrincipal;
import com.devicehive.messages.handler.HandlerCreator;
import com.devicehive.model.DeviceCommand;

import java.util.UUID;

public class CommandSubscription extends Subscription<String, DeviceCommand> {

    private final HivePrincipal principal;
    private final String commandNames;

    public CommandSubscription(HivePrincipal principal, String guid, UUID subscriptionId,
                               String commandNames,
                               HandlerCreator<DeviceCommand> handlerCreator) {
        super(guid, subscriptionId, handlerCreator);
        this.principal = principal;
        this.commandNames = commandNames;
    }

    public String getDeviceGuid() {
        return getEventSource();
    }

    public HivePrincipal getPrincipal() {
        return principal;
    }

    public String getCommandNames() {
        return commandNames;
    }

    @Override
    public String toString() {
        return "CommandSubscription{" +
                "principal=" + principal +
                ", commandNames='" + commandNames + '\'' +
                "} " + super.toString();
    }
}

