package com.devicehive.model.websockets;

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
import org.apache.commons.lang3.ObjectUtils;

import java.util.Date;

public class InsertNotification implements HiveEntity {

    @JsonPolicyDef({JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT, JsonPolicyDef.Policy.NOTIFICATION_TO_DEVICE})
    private Long id;

    @JsonPolicyDef({JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT, JsonPolicyDef.Policy.NOTIFICATION_TO_DEVICE})
    private Date timestamp;

    public InsertNotification(Long id, Date timestamp) {
        this.id = id;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return this.id;
    }

    public Date getTimestamp() {
        return ObjectUtils.cloneIfPossible(this.timestamp);
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = ObjectUtils.cloneIfPossible(timestamp);
    }
}
