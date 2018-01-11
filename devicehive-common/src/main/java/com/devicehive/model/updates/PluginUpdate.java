package com.devicehive.model.updates;

/*
 * #%L
 * DeviceHive Common Module
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


import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.enums.PluginStatus;
import com.devicehive.vo.PluginVO;
import com.google.gson.annotations.SerializedName;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.PLUGIN_PUBLISHED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.PLUGIN_SUBMITTED;
import static com.devicehive.model.enums.PluginStatus.ACTIVE;

public class PluginUpdate implements HiveEntity {

    private static final long serialVersionUID = 1499386299938178873L;

    @SerializedName("name")
    @JsonPolicyDef({PLUGIN_PUBLISHED, PLUGIN_SUBMITTED})
    private String name;

    @SerializedName("description")
    @JsonPolicyDef({PLUGIN_PUBLISHED, PLUGIN_SUBMITTED})
    private String description;

    @SerializedName("parameters")
    @JsonPolicyDef({PLUGIN_PUBLISHED, PLUGIN_SUBMITTED})
    private JsonStringWrapper parameters;

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

    public PluginVO convertTo() {
        PluginVO pluginVO = new PluginVO();
        pluginVO.setName(name);
        pluginVO.setDescription(description);
        pluginVO.setStatus(ACTIVE);
        pluginVO.setParameters(parameters);

        return pluginVO;
    }
}
