package com.devicehive.model.updates;

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

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.JsonStringWrapper;
import com.google.gson.annotations.SerializedName;

import java.util.Optional;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.EQUIPMENT_PUBLISHED;

public class EquipmentUpdate implements HiveEntity {

    private static final long serialVersionUID = -1048095377970919818L;

    @JsonPolicyDef(EQUIPMENT_PUBLISHED)
    private Long id;

    @SerializedName("name")
    @JsonPolicyDef(EQUIPMENT_PUBLISHED)
    private String name;

    @SerializedName("code")
    @JsonPolicyDef(EQUIPMENT_PUBLISHED)
    private String code;

    @SerializedName("type")
    @JsonPolicyDef(EQUIPMENT_PUBLISHED)
    private String type;

    @SerializedName("data")
    @JsonPolicyDef(EQUIPMENT_PUBLISHED)
    private JsonStringWrapper data;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public Optional<String> getCode() {
        return Optional.ofNullable(code);
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Optional<String> getType() {
        return Optional.ofNullable(type);
    }

    public void setType(String type) {
        this.type = type;
    }

    public Optional<JsonStringWrapper> getData() {
        return Optional.ofNullable(data);
    }

    public void setData(JsonStringWrapper data) {
        this.data = data;
    }
}
