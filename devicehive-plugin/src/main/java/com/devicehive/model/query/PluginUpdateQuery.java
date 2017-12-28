package com.devicehive.model.query;

/*
 * #%L
 * DeviceHive Frontend Logic
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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

import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.enums.PluginStatus;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;

import static com.devicehive.configuration.Constants.*;

public class PluginUpdateQuery extends PluginReqisterQuery {

    @ApiParam(name = STATUS, value = "Plugin status - active or disabled", defaultValue = "active")
    @QueryParam(STATUS)
    private PluginStatus status;

    @ApiParam(name = NAME, value = "Plugin name")
    @QueryParam(NAME)
    private String name;

    @ApiParam(name = DESCRIPTION, value = "Plugin description")
    @QueryParam(DESCRIPTION)
    private String description;

    @ApiParam(name = PARAMETERS, value = "Plugin parameters")
    @QueryParam(PARAMETERS)
    private JsonStringWrapper parameters;

    public PluginStatus getStatus() {
        return status;
    }

    public void setStatus(PluginStatus status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JsonStringWrapper getParameters() {
        return parameters;
    }

    public void setParameters(JsonStringWrapper parameters) {
        this.parameters = parameters;
    }

}
