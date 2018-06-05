package com.devicehive.vo;

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

import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.REST_SERVER_INFO;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.WEBSOCKET_SERVER_INFO;

/**
 * Represents meta-information about the current API. For more details see <a href="http://www.devicehive.com/restful#Reference/CacheInfoVO">CacheInfoVO</a>
 */
public class CacheInfoVO implements HiveEntity {

    private static final long serialVersionUID = 5237102920159287968L;

    @JsonPolicyDef({WEBSOCKET_SERVER_INFO, REST_SERVER_INFO})
    @Temporal(TemporalType.TIMESTAMP)
    private Date serverTimestamp;

    @JsonPolicyDef({WEBSOCKET_SERVER_INFO, REST_SERVER_INFO})
    private String cacheStats;

    public CacheInfoVO() {
    }

    public Date getServerTimestamp() {
        return ObjectUtils.cloneIfPossible(serverTimestamp);
    }

    public void setServerTimestamp(Date serverTimestamp) {
        this.serverTimestamp = ObjectUtils.cloneIfPossible(serverTimestamp);
    }

    public String getCacheStats() {
        return cacheStats;
    }

    public void setCacheStats(String cacheStats) {
        this.cacheStats = cacheStats;
    }
    
}