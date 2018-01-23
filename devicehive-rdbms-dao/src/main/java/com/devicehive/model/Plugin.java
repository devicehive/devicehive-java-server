package com.devicehive.model;

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
import com.devicehive.model.enums.PluginStatus;
import com.devicehive.vo.PluginVO;
import com.google.gson.annotations.SerializedName;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.PLUGINS_LISTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.PLUGIN_PUBLISHED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.PLUGIN_SUBMITTED;

@Entity(name = "Plugin")
@Table(name = "plugin")
@NamedQueries({
        @NamedQuery(name = "Plugin.deleteById", query = "delete from Plugin p where p.id = :id"),
        @NamedQuery(name = "Plugin.findByStatus", query = "select p from Plugin p where p.status = :status"),
        @NamedQuery(name = "Plugin.findByTopic", query = "select p from Plugin p where p.topicName = :topicName"),
        @NamedQuery(name = "Plugin.findByName", query = "select p from Plugin p where p.name = :name")
})
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Plugin implements HiveEntity {
    private static final long serialVersionUID = 8558232738863824461L;
    private static final String PLUGIN_NAME_SIZE_MESSAGE = "Field cannot be empty. The length of plugin name should be from 3 " +
            "to 128 symbols.";
    private static final String PLUGIN_NAME_PATTERN_MESSAGE = "Plugin name can contain only lowercase or uppercase letters, " +
            "numbers, and some special symbols (_@.)";
    private static final String PLUGIN_DESCRIPTION_SIZE_MESSAGE = "Field cannot be empty. The length of plugin description should be from 3 " +
            "to 128 symbols.";
    private static final String PLUGIN_DESCRIPTION_PATTERN_MESSAGE = "Plugin description can contain only lowercase or uppercase letters, " +
            "numbers, spaces and some special symbols (_@.)";

    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SerializedName("id")
    @JsonPolicyDef({PLUGIN_PUBLISHED, PLUGIN_SUBMITTED, PLUGINS_LISTED})
    private Long id;

    @Column(name = "name")
    @SerializedName("name")
    @NotNull(message = "name field cannot be null.")
    @Size(min = 3, max = 128, message = PLUGIN_NAME_SIZE_MESSAGE)
    @Pattern(regexp = "^[\\w@.-]+$", message = PLUGIN_NAME_PATTERN_MESSAGE)
    @JsonPolicyDef({PLUGIN_PUBLISHED, PLUGIN_SUBMITTED, PLUGINS_LISTED})
    private String name;

    @Column(name = "description")
    @SerializedName("description")
    @Size(min = 3, max = 128, message = PLUGIN_DESCRIPTION_SIZE_MESSAGE)
    @Pattern(regexp = "^[\\w\\s@.-]+$", message = PLUGIN_DESCRIPTION_SIZE_MESSAGE)
    @JsonPolicyDef({PLUGIN_PUBLISHED, PLUGIN_SUBMITTED, PLUGINS_LISTED})
    private String description;

    @Column(name = "topic_name")
    @SerializedName("topicName")
    @NotNull(message = "Topic name field cannot be null.")
    @Size(min = 3, max = 128, message = PLUGIN_NAME_SIZE_MESSAGE)
    @Pattern(regexp = "^[\\w@.-]+$", message = PLUGIN_NAME_PATTERN_MESSAGE)
    @JsonPolicyDef({PLUGIN_PUBLISHED, PLUGIN_SUBMITTED, PLUGINS_LISTED})
    private String topicName;

    // Filter format <notification/command/command_update>/<networkIDs>/<deviceTypeIDs>/<deviceID>/<eventNames>
    // TODO - change to embedded entity for better code readability
    @Column(name = "filter")
    @SerializedName("filter")
    @JsonPolicyDef({PLUGIN_PUBLISHED, PLUGIN_SUBMITTED, PLUGINS_LISTED})
    private String filter;

    @Column(name = "status")
    @SerializedName("status")
    @JsonPolicyDef({PLUGIN_PUBLISHED, PLUGIN_SUBMITTED, PLUGINS_LISTED})
    private PluginStatus status;

    @Column(name = "subscription_id")
    @SerializedName("subscriptionId")
    @JsonPolicyDef({PLUGIN_PUBLISHED, PLUGIN_SUBMITTED, PLUGINS_LISTED})
    private Long subscriptionId;

    @Column(name = "user_id")
    @SerializedName("userId")
    @JsonPolicyDef({PLUGIN_PUBLISHED, PLUGIN_SUBMITTED, PLUGINS_LISTED})
    private Long userId;

    @SerializedName("parameters")
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "jsonString", column = @Column(name = "parameters"))
    })
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

    public static PluginVO convertToVo(Plugin entity) {
        PluginVO vo = null;
        if (entity != null) {
            vo = new PluginVO();
            vo.setId(entity.getId());
            vo.setName(entity.getName());
            vo.setDescription(entity.getDescription());
            vo.setTopicName(entity.getTopicName());
            vo.setFilter(entity.getFilter());
            vo.setStatus(entity.getStatus());
            vo.setSubscriptionId(entity.getSubscriptionId());
            vo.setUserId(entity.getUserId());
            vo.setParameters(entity.getParameters());
        }
        
        return vo;
    }

    public static Plugin convertToEntity(PluginVO vo) {
        Plugin entity = null;
        if (vo != null) {
            entity = new Plugin();
            entity.setId(vo.getId());
            entity.setName(vo.getName());
            entity.setDescription(vo.getDescription());
            entity.setTopicName(vo.getTopicName());
            entity.setFilter(vo.getFilter());
            entity.setStatus(vo.getStatus());
            entity.setSubscriptionId(vo.getSubscriptionId());
            entity.setUserId(vo.getUserId());
            entity.setParameters(vo.getParameters());            
        }
        
        return entity;
    }
}
