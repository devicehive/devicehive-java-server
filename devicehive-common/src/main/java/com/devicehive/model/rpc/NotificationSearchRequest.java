package com.devicehive.model.rpc;

/*
 * #%L
 * DeviceHive Common Module
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

import com.devicehive.shim.api.Action;
import com.devicehive.shim.api.Body;

import java.util.Date;
import java.util.Set;

//TODO [rafa] That object to be split into two different objects. The first would contain id+deviceId, the second the rest of the fields.
public class NotificationSearchRequest extends Body {

    private Long id;
    private String deviceId;
    private Set<String> names;
    private Date timestampStart;
    private Date timestampEnd;
    private String status;

    public NotificationSearchRequest() {
        super(Action.NOTIFICATION_SEARCH_REQUEST);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Set<String> getNames() {
        return names;
    }

    public void setNames(Set<String> names) {
        this.names = names;
    }

    public Date getTimestampStart() {
        return timestampStart;
    }

    public void setTimestampStart(Date timestampStart) {
        this.timestampStart = timestampStart;
    }

    public Date getTimestampEnd() {
        return timestampEnd;
    }

    public void setTimestampEnd(Date timestampEnd) {
        this.timestampEnd = timestampEnd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
