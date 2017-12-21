package com.devicehive.model.enums;

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

public enum SearchableField {
    ID("id"),
    DEVICE_ID("deviceId"),
    TIMESTAMP("timestamp"),
    LAST_UPDATED("lastUpdated"),
    DEVICE_IDS("deviceId"), //need this duplication to separate cases of single and multiple deviceId usage
    NETWORK_IDS("networkId"),
    DEVICE_TYPE_IDS("deviceTypeId"),
    NOTIFICATION("notification"),
    COMMAND("command"),
    STATUS("status"),
    IS_UPDATED("isUpdated");

    private String field;

    SearchableField(String field) {
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
