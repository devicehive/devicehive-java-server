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

import com.devicehive.shim.api.Body;

public class CommandUpdateSubscribeRequest extends Body {

    private long commandId;
    private String deviceId;
    private String subscriptionId;

    public CommandUpdateSubscribeRequest(long commandId, String deviceId, String subscriptionId) {
        super(Action.COMMAND_UPDATE_SUBSCRIBE_REQUEST.name());
        this.commandId = commandId;
        this.deviceId = deviceId;
        this.subscriptionId = subscriptionId;
    }

    public long getCommandId() {
        return commandId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }
}
