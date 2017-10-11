package com.devicehive.vo;

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
import com.google.gson.annotations.SerializedName;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.PLUGIN_PUBLISHED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.PLUGIN_SUBMITTED;

public class PluginVO implements HiveEntity {
    private static final String PLUGIN_NAME_SIZE_MESSAGE = "Field cannot be empty. The length of plugin name should be from 3 " +
            "to 128 symbols.";
    private static final String PLUGIN_NAME_PATTERN_MESSAGE = "Plugin name can contain only lowercase or uppercase letters, " +
            "numbers, and some special symbols (_@.)";
    private static final String PLUGIN_DESCRIPTION_SIZE_MESSAGE = "Field cannot be empty. The length of plugin description should be from 3 " +
            "to 128 symbols.";
    private static final String PLUGIN_DESCRIPTION_PATTERN_MESSAGE = "Plugin description can contain only lowercase or uppercase letters, " +
            "numbers, spaces and some special symbols (_@.)";
    
    @SerializedName("id")
    @JsonPolicyDef({PLUGIN_PUBLISHED, PLUGIN_SUBMITTED})
    private Long id;

    @SerializedName("name")
    @NotNull(message = "name field cannot be null.")
    @Size(min = 3, max = 128, message = PLUGIN_NAME_SIZE_MESSAGE)
    @Pattern(regexp = "^[\\w@.-]+$", message = PLUGIN_NAME_PATTERN_MESSAGE)
    @JsonPolicyDef({PLUGIN_PUBLISHED, PLUGIN_SUBMITTED})
    private String name;

    @SerializedName("description")
    @Size(min = 3, max = 128, message = PLUGIN_DESCRIPTION_SIZE_MESSAGE)
    @Pattern(regexp = "^[\\w\\s@.-]+$", message = PLUGIN_DESCRIPTION_PATTERN_MESSAGE)
    @JsonPolicyDef({PLUGIN_PUBLISHED, PLUGIN_SUBMITTED})
    private String description;

    @SerializedName("topicName")
    @JsonPolicyDef({PLUGIN_PUBLISHED, PLUGIN_SUBMITTED})
    private String topicName;

    @SerializedName("healthCheckUrl")
    @JsonPolicyDef({PLUGIN_PUBLISHED, PLUGIN_SUBMITTED})
    private String healthCheckUrl;

    @SerializedName("healthCheckPeriod")
    @JsonPolicyDef({PLUGIN_PUBLISHED, PLUGIN_SUBMITTED})
    private Integer healthCheckPeriod;

    @SerializedName("parameters")
    @JsonPolicyDef({PLUGIN_PUBLISHED, PLUGIN_SUBMITTED})
    private JsonStringWrapper parameters;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getHealthCheckUrl() {
        return healthCheckUrl;
    }

    public void setHealthCheckUrl(String healthCheckUrl) {
        this.healthCheckUrl = healthCheckUrl;
    }

    public Integer getHealthCheckPeriod() {
        return healthCheckPeriod;
    }

    public void setHealthCheckPeriod(Integer healthCheckPeriod) {
        this.healthCheckPeriod = healthCheckPeriod;
    }

    public JsonStringWrapper getParameters() {
        return parameters;
    }

    public void setParameters(JsonStringWrapper parameters) {
        this.parameters = parameters;
    }
}
