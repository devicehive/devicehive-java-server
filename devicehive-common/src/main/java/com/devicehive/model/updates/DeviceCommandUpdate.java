package com.devicehive.model.updates;

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

import java.util.Optional;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.COMMAND_UPDATE_FROM_DEVICE;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.COMMAND_UPDATE_TO_CLIENT;

public class DeviceCommandUpdate implements HiveEntity {

    private static final long serialVersionUID = 1103746317578762591L;

    @SerializedName("status")
    @JsonPolicyDef({COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE})
    private String status;

    @SerializedName("result")
    @JsonPolicyDef({COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE})
    private JsonStringWrapper result;

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
}
