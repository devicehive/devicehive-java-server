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
import com.devicehive.model.enums.PluginStatus;
import com.google.gson.annotations.SerializedName;

import javax.persistence.Column;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.PLUGINS_LISTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.PLUGIN_PUBLISHED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.PLUGIN_SUBMITTED;

public class PluginVO implements HiveEntity {
    private static final long serialVersionUID = -4816353462582788925L;
    
    private static final String PLUGIN_NAME_SIZE_MESSAGE = "Field cannot be empty. The length of plugin name should be from 3 " +
            "to 128 symbols.";
    private static final String PLUGIN_NAME_PATTERN_MESSAGE = "Plugin name can contain only lowercase or uppercase letters, " +
            "numbers, and some special symbols (_@.)";
    private static final String PLUGIN_DESCRIPTION_SIZE_MESSAGE = "Field cannot be empty. The length of plugin description should be from 3 " +
            "to 128 symbols.";
    private static final String PLUGIN_DESCRIPTION_PATTERN_MESSAGE = "Plugin description can contain only lowercase or uppercase letters, " +
            "numbers, spaces and some special symbols (_@.)";
    
    @SerializedName("id")
    @JsonPolicyDef({PLUGIN_PUBLISHED, PLUGIN_SUBMITTED, PLUGINS_LISTED})
    private Long id;

    @SerializedName("name")
    @NotNull(message = "name field cannot be null.")
    @Size(min = 3, max = 128, message = PLUGIN_NAME_SIZE_MESSAGE)
    @Pattern(regexp = "^[\\w@.-]+$", message = PLUGIN_NAME_PATTERN_MESSAGE)
    @JsonPolicyDef({PLUGIN_PUBLISHED, PLUGIN_SUBMITTED, PLUGINS_LISTED})
    private String name;

    @SerializedName("description")
    @Size(min = 3, max = 128, message = PLUGIN_DESCRIPTION_SIZE_MESSAGE)
    @Pattern(regexp = "^[\\w\\s@.-]+$", message = PLUGIN_DESCRIPTION_PATTERN_MESSAGE)
    @JsonPolicyDef({PLUGIN_PUBLISHED, PLUGIN_SUBMITTED, PLUGINS_LISTED})
    private String description;

    @SerializedName("topicName")
    @JsonPolicyDef({PLUGIN_PUBLISHED, PLUGIN_SUBMITTED, PLUGINS_LISTED})
    private String topicName;

    @SerializedName("filter")
    @JsonPolicyDef({PLUGIN_PUBLISHED, PLUGIN_SUBMITTED, PLUGINS_LISTED})
    private String filter;

    @SerializedName("status")
    @JsonPolicyDef({PLUGIN_PUBLISHED, PLUGIN_SUBMITTED, PLUGINS_LISTED})
    private PluginStatus status;

    @SerializedName("subscriptionId")
    @JsonPolicyDef({PLUGIN_PUBLISHED, PLUGIN_SUBMITTED, PLUGINS_LISTED})
    private Long subscriptionId;

    @Column(name = "user_id")
    @SerializedName("userId")
    @JsonPolicyDef({PLUGIN_PUBLISHED, PLUGIN_SUBMITTED, PLUGINS_LISTED})
    private Long userId;

    @SerializedName("parameters")
    @JsonPolicyDef({PLUGIN_PUBLISHED, PLUGIN_SUBMITTED, PLUGINS_LISTED})
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

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public PluginStatus getStatus() {
        return status;
    }

    public void setStatus(PluginStatus status) {
        this.status = status;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public JsonStringWrapper getParameters() {
        return parameters;
    }

    public void setParameters(JsonStringWrapper parameters) {
        this.parameters = parameters;
    }
}
