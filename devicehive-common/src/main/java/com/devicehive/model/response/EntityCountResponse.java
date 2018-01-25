package com.devicehive.model.response;

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
import com.devicehive.model.rpc.CountResponse;
import com.google.gson.annotations.SerializedName;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class EntityCountResponse implements HiveEntity {

    private static final long serialVersionUID = 5041089226691783525L;

    @SerializedName("count")
    @JsonPolicyDef({USERS_LISTED, DEVICES_LISTED, NETWORKS_LISTED, DEVICE_TYPES_LISTED, PLUGINS_LISTED})
    private long count;

    public EntityCountResponse(long count) {
        this.count = count;
    }

    public EntityCountResponse(CountResponse countResponse) {
        this.count = countResponse.getCount();
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
