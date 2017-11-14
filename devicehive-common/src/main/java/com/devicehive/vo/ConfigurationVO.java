package com.devicehive.vo;

/*
 * #%L
 * DeviceHive Common Dao Interfaces
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


import com.devicehive.model.HiveEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.Column;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ConfigurationVO implements HiveEntity {
    private static final long serialVersionUID = 4259314407271953931L;
    
    @JsonProperty
    private String name;
    @Column
    @NotNull(message = "value field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of value should not be more than " +
            "128 symbols.")
    @JsonProperty
    private String value;
    @Version
    @Column(name = "entity_version")
    @JsonProperty
    private long entityVersion;

    public ConfigurationVO() {
    }

    public ConfigurationVO(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    @JsonIgnore
    public void setValue(long value) {
        this.value = Long.toString(value);
    }

    @JsonIgnore
    public void setValue(boolean value) {
        this.value = Boolean.toString(value);
    }

    public void setValue(String value) {
        this.value = value;
    }

    public long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(long entityVersion) {
        this.entityVersion = entityVersion;
    }
}
